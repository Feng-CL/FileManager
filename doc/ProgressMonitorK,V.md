# ProgressMonitor`<Key, Value>`

> <font size=4>进度监视器类，作为阻塞方法唤起另一个线程的监视类，用来接受工作线程的反馈信息，并按照一定的协议处理任务过程的可能异常</font>
>
> 具体接口方法的实现需要协定任务线程和监视对象。
>
> 通常任务线程和信息回显线程相互独立，不建议在回调函数中设置过于复杂的实现，因为这仅仅是一种信息传递，主要是为了不阻塞



| 方法                                                         | 描述                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| public void onProgress(K key,V value);                       | 总任务开始时，任务线程的回调                                 |
| public void onFinished();                                    | 总任务结束时，任务线程的回调                                 |
| public void onStart();                                       | 任务开始的回调                                               |
| public void onStop(PROGRESS_STATUS status);                  | 任务中止，并汇报终止状态PROGRESS_STATUS                      |
| public void onSubProgress(int taskId,K key,V value);         | 汇报子任务进度的回调函数                                     |
| public void onSubTaskStart(int taskId);                      | 子任务id为taskId的开始执行的回调                             |
| public void onSubTaskStop(int taskId,PROGRESS_STATUS status); | 子任务id为taskId的停止执行的回调，类似于onStop               |
| public void onSubTaskFinish(int taskId);                     | 子任务id为taskId正常完成的回调                               |
| public void receiveMessage(String msg);                      | 任务线程向监视器对象发送一段信息的接口                       |
| public void receiveMessage(int code, String msg);            | code的作用为对信息分类，具体的分类协议需要任务线程与<br />监视器一同协定 |
| public boolean interruptSignal();                            | 任务线程检查中断信号的接口                                   |
| public boolean abortSignal();                                | 任务线程检查丢弃信息                                         |



> <font size=5 color=green>public void onProgress(K key, V value)</font>

* ### Description: 

  任务进程向监视器报告任务进度的回调函数，

* ### Params:

* ### Return:

* ### Throws:

  