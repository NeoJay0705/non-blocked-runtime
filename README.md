# Non-Blocked I/O Runtime

A thread executing an I/O task will be take out from a scheduler of operating system. It means your program will be stuck until the I/O task has finished.

In a non-blocked I/O runtime, you are required to split your program into an I/O part and a callback that accepts the return from the I/O part. The number of thread will be increasing for different aspect splitted into two part CPU tasks and I/O tasks. For the I/O tasks, there are different thread pools for different devices. For example, a disk I/O should not block a socket I/O. The size of thread pool is different as well. To increase the number of threads for disk I/O is useless due to a limitation of Physics. But the socket I/O should be increased to reduce the rate of response.

# Thread Pool

A thread pool manages the number of thread that require a lambda function from a thread safe queue.

# IO Runtime

An IO runtime requires a thread pool call *CPU runtime* for CPU tasks and owns the number of thread pool for different IO types that are usually wait for I/O until the I/O has finished then packaging the result of I/O to a lambda function put to *CPU runtim*.
