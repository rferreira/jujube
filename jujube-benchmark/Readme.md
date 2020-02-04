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
FileBenchmark.measureWriteSpeedBuffered                           1  thrpt    5  209.329 ± 242.441  ops/s
FileBenchmark.measureWriteSpeedBuffered                          10  thrpt    5   53.938 ±   8.296  ops/s
FileBenchmark.measureWriteSpeedBuffered                         100  thrpt    5    2.647 ±   3.811  ops/s
FileBenchmark.measureWriteSpeedFileChannel                        1  thrpt    5   99.558 ±  26.714  ops/s
FileBenchmark.measureWriteSpeedFileChannel                       10  thrpt    5   26.756 ±  45.939  ops/s
FileBenchmark.measureWriteSpeedFileChannel                      100  thrpt    5    5.653 ±   0.295  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo              1  thrpt    5   97.710 ±   9.464  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo             10  thrpt    5   29.130 ±  18.539  ops/s
FileBenchmark.measureWriteSpeedFileChannelTransferTo            100  thrpt    5    4.170 ±   3.682  ops/s
```
