package com.scl.tallhandcamera2;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/4
 * @Description
 */
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class THEvents {

    private static THEvents thEvents;

    public static THEvents getInstance(){
        if(null == thEvents){
            thEvents = new THEvents();
        }
        return thEvents;
    }

    //todo 相机尺寸确定事件================================================================

    // 自定义事件类
    public class CameraSizeConfirmEvent extends EventObject {
        private int width;
        private int height;

        public CameraSizeConfirmEvent(Object source, int w, int h) {
            super(source);
            this.width = w;
            this.height = h;
        }

        public int getWidth(){
            return width;
        }

        public int getHeight(){
            return height;
        }
    }

    // 自定义侦听器接口
    public interface CameraSizeConfirmListener {
        void handleEvent(CameraSizeConfirmEvent event);
    }

    private List<CameraSizeConfirmListener> cameraSizeConfirmListeners = new ArrayList<>();

    // 添加监听器
    public void addTHEventListener(CameraSizeConfirmListener listener) {
        cameraSizeConfirmListeners.add(listener);
    }

    // 移除监听器
    public void removeTHEventListener(CameraSizeConfirmListener listener) {
        cameraSizeConfirmListeners.remove(listener);
    }
    
    // 触发事件
    public void dispatchEvent(int w, int h) {
        CameraSizeConfirmEvent event = new CameraSizeConfirmEvent(this, w, h);
        for (CameraSizeConfirmListener listener : cameraSizeConfirmListeners) {
            listener.handleEvent(event);  // 通知所有监听器
        }
    }

    //todo 重新给分事件 ================================================================
    public class UpdateScoreEvent extends EventObject{
        public UpdateScoreEvent(Object source){
            super(source);
        }
    }
    public interface UpdateScoreListener{
        void handleEvent(UpdateScoreEvent event);
    }
    private List<UpdateScoreListener> updateScoreListeners = new ArrayList<>();
    public void addTHEventListener(UpdateScoreListener listener){
        updateScoreListeners.add(listener);
    }
    public void removeTHEventListener(UpdateScoreListener listener){
        updateScoreListeners.remove(listener);
    }
    public void dispatchUpdateScoreEvent(){
        UpdateScoreEvent event = new UpdateScoreEvent(this);
        for(UpdateScoreListener listener : updateScoreListeners){
            listener.handleEvent(event);
        }
    }
}
