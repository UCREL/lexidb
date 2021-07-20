# LexiDB 
![Build Status](https://github.com/matthewcoole/cdb/workflows/build/badge.svg)
![codecov](https://codecov.io/gh/matthewcoole/cdb/branch/master/graph/badge.svg?token=XdKEOwSdnQ) 
![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/matthewcoole/lexidb.svg?logo=lgtm&logoWidth=18)
## Build
### Required
- [Java JDK 12](https://jdk.java.net/archive/) (In the docker build we used OpenJDK not Oracle as it is licensed under [GPLv2 + Classpath Exception](https://openjdk.java.net/legal/gplv2+ce.html)). Further in the current docker image we build with Java JDK 12 **alpine Operating System (OS)**, **but** run the LexiDB jar file using [Java JDK 16 **alpine OS** from OpenJDK](https://openjdk.java.net/projects/jdk/16/), the reason for using the Alpine OS is that it is smaller in size and [tends to have fewer security vulnerabilities compared to other Operating Systems.](https://snyk.io/blog/docker-for-java-developers/)
- [Gradle version 5.2](https://gradle.org/)

Build using the following command in the project directory;

```
$ gradle build
```


## Deploy

Deploy locally;

```
$ java -jar build/libs/lexidb-2.0.jar /path/to/app.properties
```

## Running on Docker

There is a docker instance of LexiDB which can be ran using the following command, this Docker instance was built from the [Dockerfile](./Dockerfile):

``` bash
docker run -it -p 127.0.0.1:3000:1189 --rm --init ghcr.io/ucrel/lexidb:latest
```

By default it uses the [app.properties from ./src/main/resources/app.properties](./src/main/resources/app.properties).

### Custom docker run command examples

#### Different memory settings

To run the docker instance with a custom java maximum memory allocation of 6GB:

```
docker run -it -p 127.0.0.1:3000:1189 --init --entrypoint "java" --rm ghcr.io/ucrel/lexidb:latest -Xmx6g -jar lexidb-2.0.jar ./app.properties
```

To run the docker instance with a custom java maximum memory allocation of 6GB and total docker memory usage of 8GB:

```
docker run -it -p 127.0.0.1:3000:1189 --init --entrypoint "java" --memory=8g --memory-swap=8g --rm ghcr.io/ucrel/lexidb:latest -Xmx6g -jar lexidb-2.0.jar ./app.properties
```

## Test

You can test whether the server is running by making a simple API call in your browser; [http://localhost:1189/api/test](http://localhost:1189/api/test)

## Create a corpus

Create a new corpus;

```http request
POST /mycorpus/create

{
  "name": "tokens",
  "sets": [
    {
      "name": "tokens",
      "columns": [
        {
          "name": "token"
        }
...
}
```

insert some files;

```http request
POST /mycorpus/myfile.xml/insert

token   pos sem
When	CS	Z5
it	PPH1	Z8
comes	VVZ	A4.1[i651.2.1
to	II	A4.1[i651.2.2
tropical	JJ	M7/B2-[i652.2.1
diseases	NN2	M7/B2-[i652.2.2
,	,	PUNC
future	JJ	T1.1.3
scientific	JJ	Y1
research	NN1	X2.4
...
```

finally save;

```http request
GET /mycorpus/save
```

## Query

A `GET` request can be made to the endpoint [http://localhost:1189/mycorpus/query](http://localhost:1189/mycorpus/query). The body of the request should be in the form of a JSON query;

```http request
POST /mycorpus/query

{
  "query": {
    "tokens": "{\"pos\": \"JJ\"}"
  }
}
```

This will query the `"tokens"` table and the `"pos"` (part-of-speech) column for the value `"JJ"` and return the results in the form of a `"kwic"` (keyword in context).
