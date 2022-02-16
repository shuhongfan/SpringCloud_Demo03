**一、确定ip**

进入网址[https://github.com.ipaddress.com](https://github.com.ipaddress.com/)

查看GitHub的ip地址。



```
网页中的IP github.com
```

 

**二、确定域名ip**
进入网址https://fastly.net.ipaddress.com/github.global.ssl.fastly.net



```
网页中的IP github.global.ssl.fastly.net
```

 




**三、确定静态资源ip**
进入网址https://github.com.ipaddress.com/assets-cdn.github.com


185.199.108.153 assets-cdn.github.com
185.199.110.153 assets-cdn.github.com
185.199.111.153 assets-cdn.github.com



**四、修改hosts文件**
Windows系统：打开C:\Windows\System32\drivers\etc
找到hosts文件，可以使用notepad打开，如果没有，右键选择打开方式为记事本即可。
在底部加入前三步获得的内容，即：

```
140.82.112.3 github.com
199.232.69.194 github.global.ssl.fastly.net
185.199.108.153 assets-cdn.github.com
185.199.110.153 assets-cdn.github.com
185.199.111.153 assets-cdn.github.com
```

保存并退出即可

![img](https://img2020.cnblogs.com/blog/286958/202105/286958-20210514104342873-922802666.jpg)


**五、后续步骤**
一般情况下就可以直接访问了，也有可能存在浏览器或者dns更新较慢的情况
对浏览器而言，可以关闭重启浏览器。
对DNS更新的话，可以打开cmd，输入

```
ipconfig /flushdns
```

,如下

![img](https://img2020.cnblogs.com/blog/286958/202105/286958-20210514104259729-186450115.png)

 

 现在可以试试ping功能能否到达了，能ping通说明问题已被解决~