package com.scl.tallhandcamera2.throom;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.scl.tallhandcamera2.TextActivity;

import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/7/20
 * @Description
 */
@Dao
public interface AnsDetailsDao {

    @Insert
    void insert(AnsDetails[] ansDetailsArr);

    @Query("SELECT * FROM answer_details WHERE question_number = :questionNum AND answer LIKE :search")
    AnsDetails[] findQAnsOf(int questionNum, String search);

    @Query("SELECT question_number, answer, COUNT(*) AS answer_count FROM answer_details GROUP BY question_number, answer")
    List<AnsAnalysis> analysisForAllAns();

    @Query("DELETE FROM answer_details")
    void deleteAll();

    @Query("SELECT * FROM answer_details")
    List<AnsDetails> getAll();
}
