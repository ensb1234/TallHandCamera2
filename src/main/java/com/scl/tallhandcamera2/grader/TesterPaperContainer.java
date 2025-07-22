package com.scl.tallhandcamera2.grader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.scl.tallhandcamera2.R;
import com.scl.tallhandcamera2.THEvents;
import com.scl.tallhandcamera2.ThApplication;
import com.scl.tallhandcamera2.throom.AnsDetails;
import com.scl.tallhandcamera2.throom.AnsDetailsDatabase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/4/12
 * @Description
 */
public class TesterPaperContainer extends ConstraintLayout {
    public TesterPaperContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 用这个去实例化，不要new！
     * @param parent
     * @return
     */
    public static TesterPaperContainer createInstanceIn(ConstraintLayout parent){
        //通过布局膨胀，加载xml布局
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.activity_camera2, parent, true).findViewById(R.id.testerPaperContainer);
    }

    private ImageView filledPaper;
    private ConstraintLayout testerAnswersContainer;
    private ThApplication app;
    private AnsDetailsDatabase db;
    public void initialize(){
        filledPaper = findViewById(R.id.filledPaper);
        testerAnswersContainer = findViewById(R.id.testerAnswersContainer);
        app = (ThApplication) getContext().getApplicationContext();
        db = AnsDetailsDatabase.getInstance(getContext());
    }

    @Override
    public void setVisibility(int visibility) {
        if(visibility == GONE){
            //先获取所有网格的答案并存储到数据库
            List<AnsDetails[]> ansDetailsArrList = new ArrayList<>();
            long studentId = System.currentTimeMillis();
            int qNumStartIndex = 0;
            int childCount = testerAnswersContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                ChoicesGridView child = (ChoicesGridView) testerAnswersContainer.getChildAt(i);
                AnsDetails[] ansDetailsArr = child.getAnsDetails(studentId, qNumStartIndex);
                qNumStartIndex += ansDetailsArr.length;
                ansDetailsArrList.add(ansDetailsArr);
            }
            app.getAnsExecutor().execute(()->{
                for(AnsDetails[] ansDetailsArr : ansDetailsArrList){
                    db.ansDetailsDao().insert(ansDetailsArr);
                }
            });
            //最后移除所有网格
            testerAnswersContainer.removeAllViews();
        }
        super.setVisibility(visibility);
    }

    public int addGrids(List<String> correctFeatures, List<HashSet<Integer>> testerAnsList){
        int quesCount = correctFeatures.size();
        if(quesCount != testerAnsList.size()) return 0;
        int res = 0;
        for(int i = 0; i < quesCount; i++){
            ChoicesGridView gg = new ChoicesGridView(getContext(), testerAnswersContainer, correctFeatures.get(i));
            gg.Convert2TesterMode(testerAnsList.get(i));
            testerAnswersContainer.addView(gg);
            res += gg.getTotalScore();
        }
        return res;
    }

    public int getTotalScores(){
        int res = 0;
        for(int i = testerAnswersContainer.getChildCount() - 1; i >= 0; i--){
            ChoicesGridView child = (ChoicesGridView) testerAnswersContainer.getChildAt(i);
            res += child.getTotalScore();
        }
        return res;
    }
}
