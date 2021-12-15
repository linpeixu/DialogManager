package com.cloudling.dialogmanager;

import android.util.Log;

import java.util.UUID;
import java.util.Vector;

/**
 * 描述: “概念”弹窗优先级管理（支持设置弹窗优先级）
 * 联系: 1966353889@qq.com
 * 日期: 2021/12/15
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
