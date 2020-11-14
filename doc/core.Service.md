# core.Service

> 应用与android系统对接的底层服务类

[toc]







## 这个类该做些什么？

1. 获得关键路径字符串，或者文件句柄。



## 如果系统初始化时，没有权限读取，这个类该如何向上层反馈？

因此，这里在主活动启动时，同时启动后台的对象，并根据后台对象返回的结果作进一步行动

core.Service 新增方法getState() 并罗列几个枚举量 用以标记core.Service启动情况







---





# methods API

> 这里罗列记录几个比较重要的方法API



## getInstance(Context context)

> API
>
> require API Level: 
>
> - **Description**: 
>
>   获取服务对象的句柄，需要为其指定上下文，该上下文在程序运行过程应该总保持一致。
>
> - **Parameter**:
>
>   app的上下文
>
> - **return** :
>
>   core.Service 的单体对象的引用。
>
> 





## copy(FileHandle src, String dstPath,ProgressMonitor monitor,Service_CopyOption... options)

### 可用的复制选项

1. 替换已存在的
2. 复制文件属性，last-modified-time : mtime
3. NOFOLLOW_LINKS 不跟随符号链接，默认
4. 只复制文件夹本身 ，不包含内部内容。

> API
>
> * **Description**: 复制一个文件或者文件夹到另一个路径，如果存在目标路径和源路径相同，则通知ProgressMonitor 不接受这样的请求，直到需要请求调用copy的对象修正了这样的错误。当然，若要copy接收该请求，就必须通过copy内部的可行性识别，主要的限制就是目标路径的有效性(DstFileHandle.canWrite()==true), 当然如果考虑更细致的问题 ，如果选择覆盖目标路径，也可能会导致失败的发生，因为目标路径的文件被上了锁，在锁释放前，这个覆盖写线程就会被一直悬挂，当然这不是我们希望发生的，因此如果遇到core.Service 处理不了的请求，它会即时通知ProgressMonitor 的。
>
> * **Params**: 
>
>   1. [Src: FileHandle ] : 源文件，不论是否在业务代码中已经检测了有效性，实际在复制过程中都将再次检验其有效性
>
>   2. [Dst: String] ：目标地址， 目标可写目录仅能在/storage/emulated/0 和/storage/sdcard1/下进行操作
>
>   3. [monitor:ProgressMonitor] : 持有监控能力的线程
>
>   4. [options: Service_CopyOption ] : 可选的复制选项
>
>      ```java
>          public enum Service_CopyOption{
>              REPLACE_EXISTING,   //替换已存在
>              COPY_ATTRIBUTE,    //不修改mtime
>              NOT_FOLLOWINGLINK, //不跟随符号链接
>              RECURSIVE_COPY //递归复制文件夹
>          }
>      ```
>
> * **Return**:  void
>
>   









### 存在的问题

1. API 版本差异导致复制使用的方法不一样

2. 复制的过程是否需要监视，即可监视复制进度和即时取消。

3. 复制的底层具体实现，使用系统的服务还是java的流服务。

   







