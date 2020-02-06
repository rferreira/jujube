# Benchmark Module
All tests run in a 2017 MacBook Pro 13" with 16GB of RAM and AdoptOpenJDK 11.0.3+7.
All tests had a max heap setting (-xmx) of only 64MB. 

## Results

```$text
Benchmark                                      (bufferSizeInBytes)   Mode  Cnt    Score    Error  Units
TieredOutputStreamBenchmark.measureWriteSpeed                32768  thrpt    5  287.772 ±  7.186  ops/s
TieredOutputStreamBenchmark.measureWriteSpeed                65536  thrpt    5  286.988 ±  4.344  ops/s
TieredOutputStreamBenchmark.measureWriteSpeed               524288  thrpt    5  279.139 ± 15.712  ops/s

Benchmark                                             (payloadInMB)   Mode  Cnt    Score     Error  Units
FileBenchmark.measureWriteSpeedBuffered                           1  thrpt    5  358.403 ±  31.985  ops/s
FileBenchmark.measureWriteSpeedBuffered                          10  thrpt    5   53.325 ±  15.884  ops/s
FileBenchmark.measureWriteSpeedBuffered                         100  thrpt    5    3.577 ±   0.248  ops/s
FileBenchmark.measureWriteSpeedFileChannel                        1  thrpt    5  735.931 ± 443.320  ops/s
FileBenchmark.measureWriteSpeedFileChannel                       10  thrpt    5   82.976 ±  24.444  ops/s
FileBenchmark.measureWriteSpeedFileChannel                      100  thrpt    5    8.411 ±   0.552  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo              1  thrpt    5  101.764 ±   4.472  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo             10  thrpt    5   33.406 ±   0.941  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo            100  thrpt    5    4.680 ±   0.820  ops/s

Benchmark                (payloadSizeInMB)   Mode  Cnt    Score    Error  Units
MultipartBenchmark.post                  1  thrpt    5  228.160 ± 10.942  ops/s
MultipartBenchmark.post                  5  thrpt    5   45.235 ±  2.502  ops/s
MultipartBenchmark.post                 10  thrpt    5   22.179 ±  3.523  ops/s
MultipartBenchmark.post                100  thrpt    5    2.258 ±  1.674  ops/s
```
