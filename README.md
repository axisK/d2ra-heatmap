d2ra-heatmap
============

Very basic heatmap implementation showing how to use [clarity](https://github.com/skadistats/clarity) to get the locations of all wards and plot them on a heatmap.

### Building / Usage

```
$ git clone d2ra-heatmap
$ mvn clean
$ mvn package
$ java -jar target/d2ra-heatmap-1.0-SNAPSHOT.jar \
  /path/to/replay.dem \
  /path/to/image.png
```
