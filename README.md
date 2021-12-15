# DialogManager

Android开启定时任务通用的封装
先看接入步骤：
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```java
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency
```java
    dependencies {
	        implementation 'com.github.linpeixu:DialogManager:1.0.0'
            //或者implementation 'com.gitlab.linpeixu:dialogmanager:1.0.0'
	}
```

背景：


之前我写过一篇[Android优雅实现弹窗优先级管理](https://blog.csdn.net/qq_33866343/article/details/108441576)来介绍如何在android日常开发中管理多个弹窗的优先级，这个解决方案也被其他开发者所采用，在这里也感谢大家的认可。

![在这里插入图片描述](https://img-blog.csdnimg.cn/5f23b6fc8cc0466292499e84219d1aaf.jpg?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBA54mnLueJpw==,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


我们常说世界上唯一不变的就是变化，开发的业务场景也在不停地变化，所以之前的弹窗管理解决方案在某些业务上则有场景的限制了，比如[Android优雅实现弹窗优先级管理](https://editor.csdn.net/md/?articleId=108441576)更偏向于将各个自定义的Dialog（api上的Dialog）纳入管理，且每个Dialog需要实现我们约定好的“Dialog Interface”，这样的话某种意义上已经对自定义Dialog构成代码的入侵了，再比如我们的DialogActivity也是可以实现Dialog的效果的，业务上的需要有时候需要我们使用DialogActivity来完成弹窗的业务，这个时候其实我们所说的“弹窗”的概念已经需要包括Dialog和Activity等不同组件了，由此我在思考，能不能有一种通用上的“弹窗”概念，而不仅仅局限于日常的自定义的Dialog，且不要对我们需要被管理的“弹窗”有任何代码上的入侵。

想法是美好的，开始行动，最终我重新封装了DialogManager，先看使用实例。

这里我们为了说明“弹窗”只是一个概念，需要纳入优先级管理的class不限制具体的类型，我们使用这几种不同“概念”的弹窗来看看怎么应用，例如自定义MessageDialog（继承Dialog）、AlertDialog、以及String类型的“弹窗”。

首先把对应的“概念”弹窗纳入管理只需要调用

```java
DialogManager.getInstance().add(Config<Type> config);
```

在需要展示弹窗的时候只需要调用

```java
DialogManager.getInstance().show();
```

接下来我们看看上边提到的三种“概念”弹窗是怎么纳入优先级管理的

MessageDialog“概念”弹窗的配置

```java
DialogManager.Config<MessageDialog> config1= new DialogManager
                .Config
                .Builder<MessageDialog>()
                .as(new DialogManager.BaseType<MessageDialog>(new MessageDialog(context, "第一个弹窗")) {
                    @Override
                    void init(DialogManager.Config<MessageDialog> config) {
                        config.getBaseType().getType().setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                               /*需要记住，在合适的地方一定要调用方法config.dispatch()通知DialogManager
                               *接着展示下个“概念”弹窗
                               */
                                config.dispatch();
                            }
                        });
                    }

                    @Override
                    void show(DialogManager.Config<MessageDialog> config) {
                       /*这是DialogManager执行到展示“概念”弹窗的逻辑，需外部实现相应的show逻辑*/
                        config.getBaseType().getType().show();
                    }

                    @Override
                    void dismiss(DialogManager.Config<MessageDialog> config) {
                       /*这是DialogManager执行到隐藏“概念”弹窗的逻辑，需外部实现相应的dismiss逻辑*/
                        config.getBaseType().getType().dismiss();
                    }
                })
                .priority(1)
                .onShowCheckListener(new DialogManager.OnShowCheckListener() {
                    @Override
                    public boolean isCanShow() {
                       /*这是告诉DialogManager此“概念”弹窗可否展示*/
                       /*如我们有些业务需要首页处于哪个Tab时才可展示此弹窗
                        *可以在这里自行实现判断条件
                       */
                        return true;
                    }
                })
                .build();
```

String“概念”弹窗的配置

```java
DialogManager.Config<String> config2 = new DialogManager
                .Config
                .Builder<String>()
                .as(new DialogManager.BaseType<String>("这是个文字") {
                    @Override
                    void init(DialogManager.Config<String> config) {

                    }

                    @Override
                    void show(DialogManager.Config<String> config) {
                        DialogManager.getInstance().getILog().onPrintLog("DialogManager", "show->" + config.getBaseType().getType());
                        Toast.makeText(context, "String Type show->这是个文字", Toast.LENGTH_SHORT).show();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                              /*需要记住，在合适的地方一定要调用方法config.dispatch()通知DialogManager
                               *接着展示下个“概念”弹窗
                               */
                                config.dispatch();
                            }
                        }, 3000);
                    }

                    @Override
                    void dismiss(DialogManager.Config<String> config) {
                        DialogManager.getInstance().getILog().onPrintLog("DialogManager", "dismiss->" + config.getBaseType().getType());
                    }
                })
                .priority(2)
                .onShowCheckListener(new DialogManager.OnShowCheckListener() {
                    @Override
                    public boolean isCanShow() {
                        return true;
                    }
                })
                .build();
```

AlertDialog“概念”弹窗的配置

```java
AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setMessage("第三个弹窗");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "我知道了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        DialogManager.Config<AlertDialog> config3 = new DialogManager
                .Config
                .Builder<AlertDialog>()
                .as(new DialogManager.BaseType<AlertDialog>(alertDialog) {
                    @Override
                    void init(DialogManager.Config<AlertDialog> config) {
                        config.getBaseType().getType().setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                config.dispatch();
                            }
                        });
                    }

                    @Override
                    void show(DialogManager.Config<AlertDialog> config) {
                        config.getBaseType().getType().show();
                    }

                    @Override
                    void dismiss(DialogManager.Config<AlertDialog> config) {
                        config.getBaseType().getType().dismiss();
                    }
                })
                .priority(3)
                .onShowCheckListener(new DialogManager.OnShowCheckListener() {
                    @Override
                    public boolean isCanShow() {
                        return true;
                    }
                })
                .build();
```

将三种“概念”弹窗纳入管理

```java
DialogManager.getInstance().add(config1);
DialogManager.getInstance().add(config2);
DialogManager.getInstance().add(config3);
```

在需要展示的地方只需要调用

```java
DialogManager.getInstance().show();
```

这样我们做到了对“概念”弹窗无代码入侵，待添加的“概念”弹窗对应的Config只需要在合适的地方调用方法config.dispatch()通知DialogManager接着展示下个“概念”弹窗，实现show和dismiss逻辑，实现isCanshow逻辑即可，从代码的角度上来看逻辑更加清晰。

另外，可调用如下方法设置是否开启调试模式，调试模式带日志

```java
DialogManager.getInstance().(boolean debug);
```

可调用如下方法代理调试模式下日志的输出，如

```java
DialogManager.getInstance().setILog(new DialogManager.ILog() {
                    @Override
                    public void onPrintLog(String TAG, String log) {
                        LogUtil.d(TAG, log);
                    }
                });
```

完整的DialogManager代码如下

```java
import android.util.Log;

import java.util.UUID;
import java.util.Vector;

/**
 * 作者：lpx on 2021/11/18 17:39
 * Email : 1966353889@qq.com
 * Describe:“概念”弹窗优先级管理（支持设置弹窗优先级）
 * update on 2021/11/20 11:29
 */
public class DialogManager {
    private final String TAG = "DialogManager";
    /*由于instance = new Singleton()，这并非是一个原子操作，事实上在 JVM 中这句话大概做了下面 3 件事情。
    1.给 instance 分配内存
    2.调用 Singleton 的构造函数来初始化成员变量
    3.将instance对象指向分配的内存空间（执行完这步instance就为非null了）
    但是在 JVM 的即时编译器中存在指令重排序的优化。也就是说上面的第二步和第三步的顺序是不能保证的，最终的执行顺序可能是 1-2-3 也可能是 1-3-2。如果是后者，则在 3 执行完毕、2 未执行之前，被线程二抢占了，这时 instance 已经是非 null 了（但却没有初始化），所以线程二会直接返回 instance，然后使用，然后顺理成章地报错。
    我们只需要将 instance 变量声明成 volatile 就可以了。*/
    private static volatile DialogManager mInstance;
    /**
     * 所有已添加的Type集合（线程安全的List，用法和ArrayList类似）
     */
    private Vector<Config> mConfigs = new Vector<>();
    /**
     * 当前正在显示的Type
     */
    private Config mCurrentConfig;
    /**
     * 日志输出类
     */
    private ILog mILog;
    /**
     * 是否调试模式
     */
    private boolean debug = BuildConfig.DEBUG;

    private DialogManager() {
        mILog = new ILog() {
            @Override
            public void onPrintLog(String TAG, String log) {
                Log.d(TAG, log);
            }
        };
    }

    /**
     * 打印log（内部调用）
     */
    private void printLog(String log) {
        if (debug && mILog != null) {
            mILog.onPrintLog(TAG, log);
        }
    }


    /**
     * 尝试移除相同优先级的Config
     */
    private <Type> void tryRemoveType(Config<Type> config) {
        if (mConfigs != null && !mConfigs.isEmpty() && config != null) {
            for (int i = 0, size = mConfigs.size(); i < size; i++) {
                if (mConfigs.get(i).getPriority() == config.getPriority()) {
                    mConfigs.get(i).dismiss();
                    mConfigs.remove(mConfigs.get(i));
                    break;
                }
            }
        }
    }

    /**
     * 获取已添加的最大优先级的且满足显示条件的Type
     */
    private <Type> Config<Type> getMaxPriorityCanShowType() {
        if (mConfigs != null && !mConfigs.isEmpty()) {
            int size = mConfigs.size();
            int position = size - 1;
            for (int i = size - 1; i >= 0; i--) {
                if (mConfigs.get(i).isCanShow() && mConfigs.get(i).getPriority() > mConfigs.get(position).getPriority()) {
                    position = i;
                }
            }
            return mConfigs.get(position).isCanShow() ? mConfigs.get(position) : null;
        }
        return null;
    }

    /**
     * 移除对应的Type
     */
    private <Type> void remove(Config<Type> config) {
        if (mConfigs != null && !mConfigs.isEmpty() && config != null) {
            int size = mConfigs.size();
            boolean result = mConfigs.remove(config);
            printLog("remove(object)\nresult:" + result + "\nsize:" + size + "->" + mConfigs.size());
        } else {
            printLog("remove(object)\nresult:does not perform\nsize:" + (mConfigs != null ? mConfigs.size() : "configs is null"));
        }
    }

    /**
     * 显示下个Type
     */
    private void showNext() {
        show();
    }

    /**
     * 返回当前实例
     */
    public static DialogManager getInstance() {
        if (mInstance == null) {
            synchronized (DialogManager.class) {
                if (mInstance == null) {
                    mInstance = new DialogManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 设置对应的log处理类
     */
    public void setILog(ILog log) {
        if (log != null) {
            mILog = log;
        }
    }

    public ILog getILog() {
        return mILog;
    }

    /**
     * 是否打开调试模式
     */
    public void enableDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 添加Type（支持添加相同优先级的Type）
     */
    public <Type> void add(Config<Type> config) {
        if (mConfigs != null) {
            mConfigs.add(config);
        }
    }

    /**
     * 添加Type（支持添加相同优先级的Type）
     *
     * @param priorityReplace 是否替换存在的相同优先级的Type
     */
    public <Type> void add(Config<Type> config, boolean priorityReplace) {
        if (priorityReplace) {
            tryRemoveType(config);
        }
        if (mConfigs != null) {
            mConfigs.add(config);
        }
    }


    /**
     * 显示Type
     */
    public <Type> void show() {
        Config<Type> config = getMaxPriorityCanShowType();
        if (config != null) {
            if (mCurrentConfig != null) {
                if (mCurrentConfig.getUuid().equals(config.getUuid())) {
                    printLog("show()->getMaxPriorityCanShowType()\nresult:success"
                            + "\ntype:" + (config.getBaseType() != null && config.getBaseType().getType() != null ? config.getBaseType().getType().getClass().getSimpleName() : "unknown")
                            + "\npriority:" + config.getPriority()
                            + "\nextra:the same as current type uuid");
                    return;
                }
                mCurrentConfig.closeByManager();
                mCurrentConfig.dismiss();
            }
            printLog("show()->getMaxPriorityCanShowType()\nresult:success"
                    + "\ntype:" + (config.getBaseType() != null && config.getBaseType().getType() != null ? config.getBaseType().getType().getClass().getSimpleName() : "unknown")
                    + "\npriority:" + config.getPriority());
            mCurrentConfig = config;
            config.show();
        } else {
            printLog("show()->getMaxPriorityCanShowType():\nresult:target not found");
        }
    }

    /**
     * 日志输出接口
     */
    public interface ILog {
        void onPrintLog(String TAG, String log);
    }

    /**
     * Type配置类
     */
    public static class Config<Type> {
        /**
         * 标识码
         */
        private final String uuid;
        /**
         * Type包装类（泛型）
         */
        private BaseType<Type> baseType;
        /**
         * 弹窗优先级（值越大优先级越高）
         */
        private int priority;
        /**
         * 是否通过优先级机制关闭的Type（若为true则可能重新被打开）
         */
        private boolean closeByManager;
        private OnShowCheckListener onShowCheckListener;
        private OnDismissCheckListener onDismissCheckListener;

        private Config(Builder<Type> builder) {
            uuid = UUID.randomUUID().toString();
            this.baseType = builder.baseType;
            this.priority = builder.priority;
            this.onShowCheckListener = builder.onShowCheckListener;
            this.onDismissCheckListener = builder.onDismissCheckListener;
            if (this.baseType != null) {
                this.baseType.init(this);
            }
        }

        /**
         * 当前Type是否可以show（不用考虑其它Type的情况）
         */
        private boolean isCanShow() {
            return (getBaseType() != null && getBaseType().getType() != null) && (onShowCheckListener == null || onShowCheckListener.isCanShow());
        }

        /**
         * 当前Type是否可以dismiss（不用考虑其它Type的情况）
         */
        private boolean isCanDismiss() {
            return (getBaseType() != null && getBaseType().getType() != null) && (onDismissCheckListener == null || onDismissCheckListener.isCanDismiss());
        }

        /**
         * 内部调用
         */
        private void closeByManager() {
            closeByManager = true;
        }

        /**
         * 分发事件（Type dismiss后调用）
         */
        protected void dispatch() {
            /*被优先级机制暂时隐藏的弹窗不移除*/
            if (!closeByManager) {
                DialogManager.getInstance().remove(this);
            }
            DialogManager.getInstance().showNext();
        }

        public BaseType<Type> getBaseType() {
            return baseType;
        }

        public int getPriority() {
            return priority;
        }

        public String getUuid() {
            return uuid;
        }

        /**
         * 展示当前Type
         */
        private void show() {
            if (isCanShow()) {
                /*有些Type是优先级机制暂时关闭的，重新show之后需要重置closeByManager，这样在主动dismiss当前的时候才会remove掉*/
                closeByManager = false;
                getBaseType().show(this);
            }
        }

        /**
         * 隐藏当前Type
         */
        private void dismiss() {
            if (isCanShow()) {
                getBaseType().dismiss(this);
            }
        }

        public static class Builder<Type> {
            private BaseType<Type> baseType;
            private int priority;
            private OnShowCheckListener onShowCheckListener;
            private OnDismissCheckListener onDismissCheckListener;

            public Builder<Type> as(BaseType<Type> baseType) {
                this.baseType = baseType;
                return this;
            }

            public Builder<Type> priority(int priority) {
                this.priority = priority;
                return this;
            }

            public Builder<Type> onShowCheckListener(OnShowCheckListener onShowCheckListener) {
                this.onShowCheckListener = onShowCheckListener;
                return this;
            }

            public Builder<Type> onDismissCheckListener(OnDismissCheckListener onDismissCheckListener) {
                this.onDismissCheckListener = onDismissCheckListener;
                return this;
            }

            public Config<Type> build() {
                return new Config<>(this);
            }

        }

    }

    /**
     * Type包装类
     */
    public abstract static class BaseType<Type> {
        private Type type;

        public BaseType(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        abstract void init(Config<Type> config);

        abstract void show(Config<Type> config);

        abstract void dismiss(Config<Type> config);
    }

    /**
     * 当前Type show条件检测（不用考虑其它Type的情况）-默认返回true，可外部实现更改判断条件
     */
    public interface OnShowCheckListener {
        boolean isCanShow();
    }

    /**
     * 当前Type dismiss条件检测（不用考虑其它Type的情况）-默认返回true，可外部实现更改判断条件）
     */
    public interface OnDismissCheckListener {
        boolean isCanDismiss();
    }
}
```

代码的注释挺详细的，就不做过多说明了，有问题的话可随时联系我或评论区留言。

![在这里插入图片描述](https://img-blog.csdnimg.cn/3b213c1f0ff747eaaa27d15f6b31a4d1.jpg?x-oss-process=image/watermark,type_ZHJvaWRzYW5zZmFsbGJhY2s,shadow_50,text_Q1NETiBA54mnLueJpw==,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)

