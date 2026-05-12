package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamFunctionDto {

    /**
     * 1. 鍙戝竷娴嬭瘯璇锋眰鍙傛暟
     */
    @Data
    public static class PublishRequest {
        private Long courseId;
        private String title;             // 娴嬭瘯鏍囬
        private List<String> questionIds; // 鍕鹃€夌殑棰樼洰ID鍒楄〃
        private LocalDateTime startTime;  // 寮€濮嬫椂闂?
        private LocalDateTime deadline;   // 鎴鏃堕棿
        private Integer duration;         // 鏃堕暱(鍒嗛挓)
        private Integer passScore;        // pass score
        private Long graphId;
        private String nodeId;
        private Double weight;
    }


    /**
     * [鏂板] 鏁欏笀绔?娴嬭瘯浠诲姟鎽樿
     */
    @Data
    public static class TaskSummary
    {
        private
        Long taskId;
        private
        Long paperId;
        private
        String title;
        private
        LocalDateTime startTime;
        private
        LocalDateTime deadline;

        // 鐘舵€?(0:鏈紑濮? 1:杩涜涓? 2:宸茬粨鏉?
        // 寤鸿鍦⊿ervice灞傛牴鎹綋鍓嶆椂闂村姩鎬佽绠楄繑鍥烇紝鎴栬€呯洿鎺ヨ搴?
        private
        Integer status;

        // 缁熻鏁版嵁
        private Integer submittedCount;    // 宸蹭氦浜烘暟
        private Integer totalStudentCount; // 鐝骇鎬讳汉鏁?
    }


    /**
     * [鏂板] 瀛︾敓绔?鎴戠殑娴嬭瘯浠诲姟瑙嗗浘
     */
    @Data
    public static class StudentTaskView
    {
        private
        Long taskId;
        private
        Long paperId;
        private
        String title;
        private
        LocalDateTime startTime;
        private
        LocalDateTime deadline;
        private Integer duration;      // 鑰冭瘯鏃堕暱(鍒嗛挓)

        // 浠诲姟鏈韩鐨勭姸鎬?(鍩轰簬鏃堕棿)
        // 0: 鏈紑濮?(鏃堕棿鏈埌), 1: 杩涜涓? 2: 宸叉埅姝?
        private
        Integer taskStatus;

        // 瀛︾敓涓汉鐨勭姸鎬?(鍩轰簬璁板綍)
        // 0: 鏈弬鍔?(鍙紑濮嬭€冭瘯), 1: 宸叉彁浜?寰呮壒鏀?, 2: 宸插畬鎴?鍑烘垚缁?
        private
        Integer myStatus;

        private Integer myScore;       // 鎴戠殑寰楀垎 (浠呭綋 myStatus=2 鏃舵樉绀?
    }


    /**
     * [鏂板] 瀛︾敓绔?璇曞嵎鍐呭瑙嗗浘 (涓嶅惈绛旀)
     */
    @Data
    public static class PaperView
    {
        private
        Long taskId;
        private
        Long paperId;
        private String title;          // 璇曞嵎/娴嬭瘯鏍囬
        private Integer duration;      // 鑰冭瘯鏃堕暱(鍒嗛挓)
        private LocalDateTime deadline;// 鎴鏃堕棿
        private Integer remainingSeconds; // 鍓╀綑绉掓暟 (鍙€夛紝鐢ㄤ簬鍓嶇鍊掕鏃?

        private List<QuestionItem> questions; // 棰樼洰鍒楄〃
    }

    /**
     * [鏂板] 鍗曚釜棰樼洰瑙嗗浘 (鑴辨晱鐗?
     */
    @Data
    public static class QuestionItem
    {
        private String id;             // 棰樼洰ID
        private String type;           // 鍗曢€夐/澶氶€夐/濉┖棰?绠€绛旈
        private String stem;           // 棰樺共 (鍖呭惈閫夐」鍐呭)
        private Integer score;         // 鍒嗗€?
        private Integer sortOrder;     // 棰樺彿
    }


    // ... 鍦?ExamFunctionDto 绫讳腑娣诲姞 ...

    /**
     * [鏂板] 鎻愪氦璇曞嵎璇锋眰
     */
    @Data
    public static class SubmitRequest {
        // Key: QuestionId (棰樼洰ID), Value: User Answer (瀛︾敓绛旀)
        // 娉ㄦ剰锛氬閫夐鐢ㄩ€楀彿闅斿紑(A,B)锛屽～绌洪鐢ㄥ垎鍙烽殧寮€(Answer1;Answer2)
        private Map<String, String> answers;
    }

    /**
     * [鏂板] 鎻愪氦缁撴灉鍝嶅簲
     */
    @Data
    public static class SubmitResult {
        private Integer finalScore; // 濡傛灉鍏ㄥ瑙傞锛岀洿鎺ュ嚭鍒嗭紱鍚﹀垯涓簄ull
        private String message;     // 鎻愮ず淇℃伅
    }


    // ... 鍦?ExamFunctionDto 绫讳腑 ...

    /**
     * [鏂板] 瀛︾敓鎻愪氦鎯呭喌鍒楄〃椤?
     */
    @Data
    public static class StudentSubmissionDto {
        private Long recordId;        // 鎻愪氦璁板綍ID (鏈€冨垯涓簄ull)
        private Integer studentId;
        private String studentName;
        private String studentNo;     // 瀛﹀彿/鐢ㄦ埛鍚?

        // 鐘舵€? 0:鏈紑濮? 1:杩涜涓? 2:寰呮壒鏀?宸蹭氦), 3:宸插畬鎴?鍑哄垎)
        // 娉ㄦ剰锛氳繖閲屼负浜嗗墠绔睍绀烘柟渚匡紝鎴戜滑閲嶆柊瀹氫箟浜嗕竴濂楃姸鎬佺爜锛屾垨鑰呮部鐢ㄦ暟鎹簱鐘舵€?
        // 寤鸿锛?
        // -1: 鏈弬鍔?(鏃犺褰?
        //  0: 绛旈涓?(鏈夎褰曚絾status=0)
        //  1: 寰呮壒鏀?(status=1)
        //  2: 宸插畬鎴?(status=2)
        private Integer status;

        private Integer totalScore;   // 鎴愮哗 (鏈嚭鍒嗕负null)
        private LocalDateTime submitTime; // 鎻愪氦鏃堕棿 (鍙?record.updatedAt 鎴?createdAt)
    }

    // ... 鍦?ExamFunctionDto 绫讳腑 ...

    /**
     * [鏂板] 闃呭嵎瑙嗗浘 (鏁欏笀绔?
     */
    @Data
    public static class GradingView {
        private Long recordId;
        private Integer studentId;
        private String studentName;
        private Integer totalScore;    // 褰撳墠鎬诲垎
        private Integer fullScore;     // 鍗烽潰婊″垎 (鍙€?
        private List<GradingQuestionItem> questions;
    }

    /**
     * [鏂板] 闃呭嵎棰樼洰鏄庣粏
     */
    @Data
    public static class GradingQuestionItem {
        private String id;             // 棰樼洰ID
        private String stem;           // 棰樺共
        private String type;           // 棰樺瀷
        private Integer score;         // 鏈婊″垎
        private Integer sortOrder;     // 棰樺彿

        private String studentAnswer;  // 瀛︾敓濉啓鐨勭瓟妗?
        private String standardAnswer; // 鏍囧噯绛旀
        private Integer gainedScore;   // 瀛︾敓瀹為檯寰楀垎
        private Boolean isCorrect;     // 鏄惁鑷姩鍒ゅ畾涓烘纭?

        // 杈呭姪瀛楁锛氭槸鍚︿负涓昏棰?(鐢ㄤ簬鍓嶇楂樹寒闇€瑕佷汉宸ユ壒鏀圭殑棰?
        private Boolean isSubjective;
    }


// ... 鍦?ExamFunctionDto 绫讳腑 ...

    /**
     * [鏂板] 鏁欏笀璇勫垎璇锋眰
     */
    @Data
    public static class GradeRequest {
        // 鏀寔鎵归噺鎻愪氦澶氶亾棰樼殑鍒嗘暟
        private List<QuestionGrade> grades;

        @Data
        public static class QuestionGrade {
            private String questionId; // 棰樼洰ID
            private Integer score;     // 鏁欏笀缁欏嚭鐨勫垎鏁?
        }
    }


    // ... 鍦?ExamFunctionDto 绫讳腑 ...

    /**
     * [鏂板] 瀛︾敓鑰冨悗缁撴灉瑙嗗浘
     */
    @Data
    public static class StudentResultView {
        private Long taskId;
        private String title;
        private Integer totalScore;    // 瀛︾敓鎬诲垎
        private Integer fullScore;     // 璇曞嵎婊″垎
        private List<ResultQuestionItem> questions;
    }

    /**
     * [鏂板] 缁撴灉棰樼洰鏄庣粏
     */
    @Data
    public static class ResultQuestionItem {
        private String id;             // 棰樼洰ID
        private String stem;           // 棰樺共
        private String type;           // 棰樺瀷
        private Integer score;         // 鏈婊″垎
        private Integer sortOrder;     // 棰樺彿

        private String studentAnswer;  // 瀛︾敓鍐欑殑
        private String standardAnswer; // 鏍囧噯绛旀
        private String analysis;       // 瑙ｆ瀽 (鏍稿績浠峰€?

        private Integer gainedScore;   // 寰楀垎
        private Boolean isCorrect;     // 鏄惁姝ｇ‘
    }


    // ... 鍦?ExamFunctionDto 绫讳腑 ...

    /**
     * [鏂板] 瀛︾敓鍔犲叆鐨勭彮绾т俊鎭?
     */
    @Data
    public static class MyClassInfo {
        private Long classId;
        private String className;   // 鐝骇鍚嶇О (濡? 2024绉嬪Java涓€鐝?

        private Long courseId;
        private String courseName;  // 璇剧▼鍚嶇О (濡? Java绋嬪簭璁捐)

        private String teacherName; // 浠昏鏁欏笀 (鍙€?
    }
}



