package com.dr.saloon.notifications;

import android.app.Activity;


public class Notification {

    public static void SendNotificationToTopic(String title, String description,String topic, Activity activity){
        FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender("/topics/"+topic
                ,title,description,
                activity, activity);
        fcmNotificationsSender.SendNotifications();
    }

}
