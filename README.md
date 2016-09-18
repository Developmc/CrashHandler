# Android CrashHandler
### 作用
当程序发生崩溃时，捕抓出错信息，并显示出来，帮助非Debug模式下定位错误。
### 截图
![Image](https://github.com/Developmc/CrashHandler/blob/master/app/src/main/res/drawable/crash.png) 

### Using Android Studio
Step 1. Add it in your **root build.gradle** at the end of repositories:
``` groovy
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
Step 2. Edit your **App build.gradle** file and add below dependency:
``` groovy
dependencies {
    compile 'com.github.Developmc:CrashHandler:1.0.4'
}
```
### How to use
```java
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.init(this,true);
    }
}
```
