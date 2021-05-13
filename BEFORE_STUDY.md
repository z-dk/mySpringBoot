### 编译构建SpringBoot源码项目:V2.1.x
1. 禁用maven的代码检查，在跟pom中增加一下配置即可
```xml
<properties>
    <revision>2.1.14.BUILD-SNAPSHOT</revision>
    <main.basedir>${basedir}</main.basedir>
    <!--  设置disable.checks为true  -->
    <disable.checks>true</disable.checks>
</properties>
```
2. 执行以下命令来编译构建源码项目
```shell
mvn clean install -DskipTests -Pfast
```
3. 运行SpringBoot自带的sample 
  > 默认spring-boot-samples没有被添加到跟pom中，将其添加到跟pom中