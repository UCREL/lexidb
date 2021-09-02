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

For more detail on the configuration settings within [app.properties see the app properties section below.](#app-properties)

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

#### Formatting / Importing data

If you would like to import data into LexiDB without having to use the web API, you can do this through the [java insert script](./src/main/java/util/Insert.java). The java insert script converts the data files you want to import into a format that LexiDB can read. The insert script takes 4 arguments:

1. File path to a `app.properties` file.
2. Name of the corpus / database. This is equivalent to the name of the database in a MySQL database.
3. File path to the corpus configuration file.
4. File path to the files to insert. The files are expected to be in `tsv` format, for more information on the format of the files see this [guide](https://github.com/matthewcoole/lexidb/wiki/Creating-a-schema).

``` bash
docker run -v $(pwd)/test_data:/lexidb/lexi-data --entrypoint "java" --rm ghcr.io/ucrel/lexidb:latest -cp lexidb-2.0.jar util/Insert /lexidb/lexi-data/app.properties example /lexidb/lexi-data/.conf.json /lexidb/lexi-data
```

In the command above we have created a new database called `example` whereby the [`/lexidb/lexi-data/app.properties`](./test_data/app.properties) states that this `example` corpus will be stored on the docker container in the folder `/lexidb/data` within the folder `/lexidb/data/example`. 

### Build Docker

If you would like to build the docker image locally:

``` bash
docker build -t NAME:TAG .
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

## App Properties

The app.properties file should be a JSON file with the following keys, if any of keys are missing in the file the default value will be used, if no file is given the default values will be used:

| Key | Default Value | Description |
|-----|---------------|-------------|
| `block.cache.size` | 100 | |
| `block.cache.timeout` | 1000 | |
| `corpus.cache.size` | 10 | |
| `corpus.cache.timeout` | 1000 | |
| `result.cache.size` | 100 | |
| `result.cache.timeout` | 30 | |
| `data.path` | lexi-data | Relative or absolute file path to the top level directory that LexiDB will use to store new and/or current data, if the directory does not exist it will create the directory. For more details on how to format / import data into LexiDB see the [formatting / importing data section above.](#formatting--importing-data) |
| `kwic.context` | 5 | Default context size for Key Word In Context (KWIC) searches. With the default this would result in 5 words before and after the key word. |
| `result.page.size` | 100 | Default number of KWIC results to display per page when querying the KWIC API. |
| `block.size` | 10000000 | The number of words to store per block within LexiDB. The large this number is the more memory (RAM) your machine will require, but it will increase the speed of your queries. |

## Performance

One of the main key performance bottle necks with respect to query speed is the `block.size` that is set within [app.properties](#app-properties). The larger the block size the faster the querying, but it will require more memory (RAM).

### Issues

If you see an error like the one below, full error output can be found in the [example_performance_error.txt file](./example_performance_error.txt), then this is likely to be due to not having enough RAM allocated to the Java Virtual Machine (JVM). To increase the RAM allocation to the JVM use the `-Xmx` flag, on most Ubuntu machine the default value for `-Xmx` is ~4GB to increase it to 6GB use `-Xmx6g`.

``` bash
lexi_1  | 2021-08-24 07:29:19 ERROR server.Server:175 - QUERY FAILED!
lexi_1  | com.fasterxml.jackson.databind.exc.MismatchedInputException: No content to map due to end-of-input
lexi_1  |  at [Source: (String)""; line: 1, column: 0]
lexi_1  | 	at com.fasterxml.jackson.databind.exc.MismatchedInputException.from(MismatchedInputException.java:59)
lexi_1  | 	at com.fasterxml.jackson.databind.ObjectMapper._initForReading(ObjectMapper.java:4134)
lexi_1  | 	at com.fasterxml.jackson.databind.ObjectMapper._readMapAndClose(ObjectMapper.java:3988)
lexi_1  | 	at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:2992)
lexi_1  | 	at io.javalin.translator.json.JavalinJacksonPlugin.toObject(Jackson.kt:27)
lexi_1  | 	at io.javalin.Context.bodyAsClass(Context.kt:81)
```
