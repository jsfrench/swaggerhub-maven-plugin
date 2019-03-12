[![Build Status](https://img.shields.io/jenkins/s/https/jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swaggerhub-maven-plugin.svg)](https://jenkins.swagger.io/view/OSS%20-%20Java/job/oss-swaggerhub-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.swagger/swaggerhub-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/io.swagger/swaggerhub-maven-plugin)
# swaggerhub-maven-plugin <img src="https://raw.githubusercontent.com/swagger-api/swagger.io/wordpress/images/assets/SW-logo-clr.png" height="50" align="right">
A simple maven plugin to access [SwaggerHub](https:\\swaggerhub.com) hosting of [OpenAPI/Swagger](https://swagger.io/specification/) definitions with a maven build process, using the [SwaggerHub API](https://app.swaggerhub.com/apis/swagger-hub/registry-api).

## Features
* Download/upload API definitions from/to SwaggerHub.
* Supports `json` and `yaml` format for API definitions.
* Authenticate with API key for restricted operations (e.g downloading a private API definition). 
* Connects to SwaggerHub cloud by default or local SwaggerHub instance through optional configuration.

The pattern of usage is likely to depend on whether a [code first or design first](https://swaggerhub.com/blog/api-design/design-first-or-code-first-api-development/) approach is followed.

## Example use cases

### Code First
1. Code API implementation.
2. Automatically generate API definition from implementation, e.g. via [swagger-core](https://github.com/swagger-api/swagger-core) [annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations) and [swagger maven plugin](https://github.com/swagger-api/swagger-core/tree/master/modules/swagger-maven-plugin). See also [swagger-core wiki](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Getting-started)
3. Upload generated API definition to SwaggerHub with swaggerhub-maven-plugin.

### Design First
1. Write API definition (e.g. in Swagger Editor or SwaggerHub).
2. Download API definition with swaggerhub-maven-plugin.
3. Pass API definition to another Swagger tool e.g.
    - [swagger-codegen-maven-plugin](https://github.com/swagger-api/swagger-codegen/tree/master/modules/swagger-codegen-maven-plugin) to generate API client and resource classes.
    - [swagger-inflector](https://github.com/swagger-api/swagger-inflector) to automatically wire up the API definition to the implementation and provide out-of-the-box mocking.



## Goals
### download
#### Example Usage
* Download a public API definition in json format from SwaggerHub automatically as part of the default maven build lifecycle and save to a local file.
```xml
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>generate-resources</phase>
                <goals>
                    <goal>download</goal>
                </goals>
                <configuration>
                    <api>PetStoreAPI</api>
                    <owner>jsfrench</owner>
                    <version>1.0.0</version>
                    <outputFile>target/petStoreAPI.json</outputFile>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

#### Parameters
Parameter | Description | Required | Default
--------- | ----------- | --------- | -------
**`api`** | API name | true  | -
**`owner`** | API owner | true | -
**`version`** | API version | true | -  
**`outputFile`** | API definition is written to this file | true | - 
**`token`** | SwaggerHub API key, required to access private definitions | false | -
**`format`** | API definition format, `json` or `yaml` | false | `json`
**`host`** | URL of SwaggerHub API | false | `api.swaggerhub.com`
**`protocol`** | Protocol for SwaggerHub API,`http` or `https` | false | `https`
**`port`** | Port to access SwaggerHub API| false | `443`

***

### upload

Definitions can be uploaded by one of 2 ways:

* Input file - One definition specified by file path
* Directory - One to many definitions residing in a specified directory, filtered by an optional regex patten

##### inputFile

When uploading by input file, the following parameters are required: `api`, `version` and `inputFile` are required.

##### directory

Similarly by directory, the following parameters are required: `definitionDirectory`. `definitionFileNameRegex` is optional

API name and version will both be generated by parsing the definition. API name will be the title of the definition with any non word characters replaced by underscores. 
Version will be pulled directly from the definition.

Definitions that don't have one of the following file extension types will be ignored: `yaml`, `yml`, `json`

 `definitionFileNameRegex` attempts to match on the filename without file extension. 
 If this parameter isn't specified, there will be an attempt to upload each file within the directory.
 
 If any one of the definitions in the specified directory are unable to be uploaded, the build will fail and subsequent definitions will not be uploaded.

### SwaggerHub SCM Integrations

On upload, there is the option to automatically create an [SCM Integration](https://app.swaggerhub.com/help/integrations/index) 
which will allow changes to the definition to be pushed back to source control. This functionality is available for both input file and directory based upload.

The user must define the following parameters to configure such an integration: `scmProvider`, `scmToken`. `repository`, `repositoryOwner`, `branch`

#### Example Usage
* Upload a single API definition in json format as a public API in SwaggerHub via a specified input file.
```xml
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>upload</goal>
                </goals>
                <configuration>
                    <api>PetStoreAPI</api>
                    <owner>jsfrench</owner>
                    <version>1.0.1-SNAPSHOT</version>
                    <inputFile>target/petStoreAPI.json</inputFile>
                    <token>${SWAGGERHUB_APIKEY}</token>
                    <uploadType>inputFile</uploadType>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

#### Example Usage together with `swagger-maven-plugin` (code first)


```xml
    <plugin>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-maven-plugin</artifactId>
        <version>2.0.5</version>
        <configuration>
            <outputFileName>petStoreAPI</outputFileName>
            <outputPath>${project.build.directory}</outputPath>
            <outputFormat>JSON</outputFormat>
            <resourcePackages>
                <package>test.petstore</package>
            </resourcePackages>
            <prettyPrint>TRUE</prettyPrint>
        </configuration>
        <executions>
            <execution>
                <phase>compile</phase>
                <goals>
                    <goal>resolve</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>upload</goal>
                </goals>
                <configuration>
                    <api>PetStoreAPI</api>
                    <owner>jsfrench</owner>
                    <version>1.0.1-SNAPSHOT</version>
                    <inputFile>target/petStoreAPI.json</inputFile>
                    <token>${SWAGGERHUB_APIKEY}</token>
                    <uploadTpe>inputFile</uploadTpe>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

#### Example Usage
* Upload multiple API definitions via a specified directory.
```xml
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>upload</goal>
                </goals>
                <configuration>
                    <owner>jsfrench</owner>
                    <token>${SWAGGERHUB_APIKEY}</token>
                    <uploadType>directory</uploadType>
                    <definitionDirectory>${project.basedir}/api-definitions</definitionDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>
```
* Upload multiple API definitions via a specified directory and regex pattern to filter.
```xml
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>upload</goal>
                </goals>
                <configuration>
                    <owner>jsfrench</owner>
                    <token>${SWAGGERHUB_APIKEY}</token>
                    <uploadType>directory</uploadType>
                    <definitionDirectory>${project.basedir}/api-definitions</definitionDirectory>
                    <definitionFileNameRegex>^definition\w*</definitionDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>
```
* Upload multiple API definitions via a specified directory and configure GitHub integrations for each definition
```xml
    <plugin>
        <groupId>io.swagger</groupId>
        <artifactId>swaggerhub-maven-plugin</artifactId>
        <version>1.0.3-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>upload</goal>
                </goals>
                <configuration>
                    <owner>jsfrench</owner>
                    <token>${SWAGGERHUB_APIKEY}</token>
                    <uploadType>directory</uploadType>
                    <definitionDirectory>${project.basedir}/api-definitions</definitionDirectory>
                    <scmToken>${GITHUB_APIKEY}</scmToken>
                    <scmProvider>GITHUB</scmProvider>
                    <repository>my_definitions_repository</repository>
                    <repositoryOwner>githubUser</repositoryOwner>
                    <branch>develop</branch>
                </configuration>
            </execution>
        </executions>
    </plugin>
```

#### Parameters
Parameter | Description | Required | Default
--------- | ----------- | --------- | -------
**`api`** | API name | false  | -
**`owner`** | API owner | false | -
**`version`** | API version | false | -  
**`inputFile`** | Local file containing the API definition in json or yaml format  | false | - 
**`token`** | SwaggerHub API key | true | -
**`format`** | API definition format, `json` or `yaml` | false | `json`
**`isPrivate`** | Defines whether the API should be private on SwaggerHub (using `true` requires a paid plan) | false | `false`
**`host`** | URL of SwaggerHub API | false | `api.swaggerhub.com`
**`protocol`** | Protocol for SwaggerHub API,`http` or `https` | false | `https`
**`port`** | Port to access SwaggerHub API| false | `443`
**`definitionDirectory`** | Directory containing definitions for upload | false | -
**`definitionFileNameRegex`** | Regex pattern used to filter definition filenames in the definition directory| false | -
**`uploadType`** | Upload of a single definition via `inputFile` or multiple definitions stored in a directory via `directory`| true | -
**`scmToken`** | The API token for the chosen SCM| false | -
**`scmProvider`** | The SCM provider to integrate with| false | -
**`repository`** | The repository to which the SCM integration is configured to push to | false | -
**`repositoryOwner`** | The owner(SCM account) of the repository to which the integration is configured to push to| false | -
**`branch`** | The branch to which the SCM integration is configured to push to| false | SWAGGERHUB
**`enableScmIntegration`** | Enable the SCM integration once created| false | true
**`skipFailures`** | If failures are encountered on definition upload or plugin set up, proceed with the build.| false | false



