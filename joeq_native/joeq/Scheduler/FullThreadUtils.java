package Scheduler;

import Clazz.jq_Class;
import Main.jq;
import Run_Time.Reflection;

public class FullThreadUtils implements ThreadUtils.Delegate {
    public Scheduler.jq_Thread getJQThread(java.lang.Thread t) {
        if (!jq.RunningNative) {
            jq_Class k = Bootstrap.PrimordialClassLoader.getJavaLangThread();
            Clazz.jq_InstanceField f = k.getOrCreateInstanceField("jq_thread", "LScheduler/jq_Thread;");
            return (Scheduler.jq_Thread)Reflection.getfield_A(t, f);
        }
        return ((ClassLib.Common.InterfaceImpl)ClassLib.ClassLibInterface.DEFAULT).getJQThread(t);
    }    
}