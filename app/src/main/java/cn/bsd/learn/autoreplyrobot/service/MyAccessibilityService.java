package cn.bsd.learn.autoreplyrobot.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import cn.bsd.learn.autoreplyrobot.utils.HttpUtils;

public class MyAccessibilityService extends AccessibilityService {
    private String name;
    private String contents;

    /**
     * 1、获取到聊天内容
     * 2、跳转到微信的聊天页面
     * 3、找到输入框输入需要回复的内容
     * 4、找到发送按钮，点击
     * @param event
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //获取事件类型
        int eventType = event.getEventType();
        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                //微信有新消息
                List<CharSequence> texts = event.getText();
                if(!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if(!TextUtils.isEmpty(content)){
                            //可以获取到用户的消息内容了userName:content
                            //跳转到微信的聊天页面
                            sendNotificationReply(event);
                        }
                    }
                }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    //窗口状态变更
                    //3、找到输入框输入需要回复的内容
                    //获取页面节点信息
                    fill();
                    break;
        }
    }

    private void fill() {
        MyTask myTask = new MyTask();
        myTask.execute();

    }

    private void send() {
        //重新获取页面的节点信息
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        //
        if(rootNode!=null){
            List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText("发送");
            if (list != null&&list.size()>0) {
                for (AccessibilityNodeInfo nodeInfo : list) {
                    if(nodeInfo.getClassName().equals("android.widget.Button")&&nodeInfo.isEnabled()){
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }else{
                List<AccessibilityNodeInfo> lists = rootNode.findAccessibilityNodeInfosByText("Send");
                if (lists != null&&lists.size()>0) {
                    for (AccessibilityNodeInfo nodeInfo : lists) {
                        if(nodeInfo.getClassName().equals("android.widget.Button")&&nodeInfo.isEnabled()){
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
        }
        //返回到主界面
        backToHome();

    }

    private void backToHome() {
//        performGlobalAction()
        Intent home=new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        //找到输入框输入需要回复的内容
        int childCount = rootNode.getChildCount();
        //遍历查找
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo nodeChild = rootNode.getChild(i);
            if(nodeChild==null){
                continue;
            }
            if(nodeChild.getClassName().equals("android.widget.EditText")){
                //已经找到了输入框
                //输入需要回复的内容
                nodeChild.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                //将文本输入到剪贴板
                ClipData clipData = ClipData.newPlainText("label",content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clipData);
                nodeChild.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }
            if(findEditText(nodeChild,content)){
                return true;
            }
        }
        return false;
    }

    private void sendNotificationReply(AccessibilityEvent event) {

        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
            //获取到通知栏
            Notification notification = (Notification) event.getParcelableData();
            //获取消息内容
            String content = notification.tickerText.toString();
            //数据拆分userName:content
            int index = content.indexOf(":");
            name = content.substring(0,index);
            contents = content.substring(index+1);
            //跳转到微信的聊天页面
            PendingIntent contentIntent = notification.contentIntent;
            try {
                contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    class MyTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            //网络请求

            return HttpUtils.autoReply(contents);
        }

        @Override
        protected void onPostExecute(Object o) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if(rootNode!=null){
                //判断收到的消息（去选择回复什么消息）
                if (findEditText(rootNode,o!=null?o.toString():"老子不想理你")) {
                    //找到发送按钮，点击
                    send();
                }
            }
            super.onPostExecute(o);
        }
    }
}
