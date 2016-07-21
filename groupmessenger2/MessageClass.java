package edu.buffalo.cse.cse486586.groupmessenger2;
import java.util.Comparator;
/**
 * Created by talem on 7/8/2016.
 */
public class MessageClass {
    public String msgType = null;
    public String msgText = null;
    public int msgPriority;
    public int msgProcNum;
    public int msgFromProcess;
    public int receipt;
    public int msgId;
    public String deliverable;

    public MessageClass() {
        this.receipt = 0;
        this.deliverable = "N";
    }

    public static Comparator<MessageClass> priorityComparator = new Comparator<MessageClass>() {
        public int compare(MessageClass m1,MessageClass m2) {
            int priority1 = m1.msgPriority;
            int processNum1 = m1.msgProcNum;
            int processNum2 = m2.msgProcNum;
            int msgID1 = m1.msgId;
            int msgID2 = m2.msgId;
            int priority2 = m2.msgPriority;
            if (priority1 == priority2) {
                return processNum1-processNum2;
            }else {
                return priority1-priority2;
            }
        }
    };

    public String getToString() {
        return msgType + "::"+
                msgPriority +"::"+
                msgProcNum +"::"+
                msgId +"::"+
                msgFromProcess+"::"+
                deliverable +"::"+
                msgText;
    }

}
