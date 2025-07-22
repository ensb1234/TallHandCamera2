package com.scl.tallhandcamera2.throom;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/7/20
 * @Description
 */
@Database(entities = {AnsDetails.class}, version = 1, exportSchema=false)
public abstract class AnsDetailsDatabase extends RoomDatabase {
    public abstract AnsDetailsDao ansDetailsDao();
    private static volatile AnsDetailsDatabase INSTANCE;
    public static AnsDetailsDatabase getInstance(Context context){
        if(INSTANCE == null){
            synchronized (AnsDetailsDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AnsDetailsDatabase.class, "ans-database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
