package com.scl.tallhandcamera2.throom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/7/20
 * @Description
 */
@Entity(tableName = "answer_details",
        indices = {@Index(value ={"student_id", "question_number"}, unique = true)})
public class AnsDetails {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "student_id")
    public long studentId;

    @ColumnInfo(name = "question_number")
    public int questionNum;

    public String answer;

//    public int score;
//
//    @ColumnInfo(name = "answer_bingo")
//    public String ansBingo;
//
//    @ColumnInfo(name = "answer_wrong")
//    public String ansWrong;
//
//    @ColumnInfo(name = "answer_missing")
//    public String ansMissing;

    public AnsDetails(long studentId, int questionNum, String answer){
        this.studentId = studentId;
        this.questionNum = questionNum;
        this.answer = answer;
    }
}
