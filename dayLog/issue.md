## ISSUE&TODO：

* 如何知道对方中断了TCP连接，TCP连接的关闭过程应该是怎样的呢？
* 修改Client-Server架构，服务器与客户端应该反过来
* 应该先创建好文件夹后再进行传送，但这只是在发送ACK前应当完成的工作，注意职责分配。



### 2021/2/11(除夕当晚)

* 流关闭不同步问题，容易导致一边处于死锁状态。
* 消除可能存在的死锁循环



### 2021/2/12(Spring festival)

* `$FileNodeWrapper.iterator()`迭代器的next方法可能还有bug,需要拉出来进行单独测试，总进度显示异常，原因是询问时没有调用正确的size方法
* 重命名时的异常闪退问题
* 移动文件时，调用的makeDirectory方法会造成闪退，原因不明。
* 界面设计问题，网络中是否启用发现自己的问题，还有scan方法的实现更改。