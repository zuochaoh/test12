package com.ruoyi.quartz.ctp.event;

import com.ruoyi.quartz.ctp.method.Method;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @Description 事件引擎
 * @Author gongtan
 * @Date 2019/11/20 15:15
 * @Version 1.0
 **/
public class EventEngine {

    /**
     * 事件引擎： 描述，有序且高效的处理所有事件的发生以及处理
     */


    //事件队列
    private LinkedBlockingQueue<Event> queue;

    // 事件引擎开关
    private boolean active;
    // 事件处理线程
    private Thread thread;
    // 计时器，用于触发计时器事件
    private Thread timer;
    // 计时器工作状态
    private boolean timerActive;
    // 计时器触发间隔（默认1秒）
    private int timerSleep;

    //这里的__handlers是一个字典，用来保存对应的事件调用关系
    private Map<String, List<Method>> handlers;

    //__generalHandlers是一个列表，用来保存通用回调函数（所有事件均调用）
    private List<Method> generalHandlers;




    // 初始化事件引擎
    public EventEngine() {
        // 事件队列
        this.queue = new LinkedBlockingQueue<Event>();
        // 事件引擎开关
        this.active = false;

        // 事件处理线程
        this.thread = new Thread() {
            @Override
            public void run() {
                engineRun();
            }
        };

        // 计时器，用于触发计时器事件
        this.timer = new Thread() {
            @Override
            public void run() {
                runTimer();
            }
        };
        this.timerActive = false; // 计时器工作状态
        this.timerSleep = 1000; // 计时器触发间隔（默认1秒）

        // 这里的__handlers是一个字典，用来保存对应的事件调用关系
        // 其中每个键对应的值是一个列表，列表中保存了对该事件进行监听的函数功能
        this.handlers = new ConcurrentHashMap<String, List<Method>>();

        // __generalHandlers是一个列表，用来保存通用回调函数（所有事件均调用）
        this.generalHandlers = new CopyOnWriteArrayList<Method>();
    }



    /**
     * @param
     * @return void
     * @Description 定时器事件 Timer事件的生成  此处为开辟的一个线程 与其他线程互不干扰 结束线程时，调用join方法 处理完再结束
     * @author gt_vv
     * @date 2019/11/20
     */
    private void runTimer() {
        //判断引擎是否开启
        while (this.active) {
            //开启后生成 Timer事件并放入事件队列中等待被处理
            Event event = new Event(EventType.EVENT_TIMER);  //event 目前代表一个 Timer事件
            //放入对列中 等待事件的处理
            this.put(event);
            //设置线程睡眠 (默认1s) 后-->在执行此循环
            try {
                //timerSleep = 1000
                Thread.sleep(this.timerSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param event
     * @return void
     * @Description 向事件队列中存入事件
     * @author gt_vv
     * @date 2019/11/20
     */
    public void put(Event event) {
        this.queue.offer(event);
    }

    /**
     * @param
     * @return void
     * @Description 引擎运行线程   与事件生成线程互不干涉  结束时候调用 join 方法 执行完线程 再结束
     * @author gt_vv
     * @date 2019/11/20
     */
    private synchronized void engineRun() {
        //判断 引擎是否被开启
        while (this.active) {
            try {
                //向队列索求  弹出一个事件（有序）
                Event event = this.queue.poll(1000, TimeUnit.MILLISECONDS);
                if (event == null) {
                    //事件为空 跳出本次循环 ，有事件 则进入事件处理方法
                    continue;
                }

                //处理事件
                this.process(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param event
     * @return void
     * @Description 事件处理 主方法
     * @author gt_vv
     * @date 2019/11/20
     */
    private void process(Event event) {
        // 检查是否存在对该事件进行监听的处理函数
        if (this.handlers.containsKey(event.getEventType())) {
            // 若存在，则按顺序将事件传递给处理函数执行  handlers  map 集合
            for (Method handler : this.handlers.get(event.getEventType())) {
                //执行 回调函数
                handler.invoke(event);
            }
        }

        // 调用通用处理函数进行处理
        if (this.generalHandlers != null) {
            for (Method handler : this.generalHandlers) {
                handler.invoke(event);
            }
        }
    }


    public void start() {
        System.out.println("引擎启动");
        start(true);
    }


    /**
     * 引擎启动
     * @author gt_vv
     * @param timer
     */
    public void start(boolean timer) {
        // 将引擎设为启动
        this.active = true;

        // 启动事件处理线程
        this.thread.start();

        // 启动计时器，计时器事件间隔默认设定为1秒
        System.out.println("引擎启动");
        if (timer) {
            this.timerActive = true;
            this.timer.start();
        }
    }


    /**
     * 引擎停止调用stop方法   设置引擎参数 并且 在线程运行完后停止进程
     */
    public void stop() {
        // 将引擎设为停止
        this.active = false;

        // 停止计时器
        this.timerActive = false;
        try {
            this.timer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 等待事件处理线程退出
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




    // 注册事件处理函数监听
    public void register(String type_, Method handler) {
        // 尝试获取该事件类型对应的处理函数列表，若无defaultDict会自动创建新的list
        List<Method> handlerList = this.handlers.getOrDefault(type_, new CopyOnWriteArrayList<Method>());

        // 若要注册的处理器不在该事件的处理器列表中，则注册该事件
        if (!handlerList.contains(handler)) {
            handlerList.add(handler);
        }
        this.handlers.putIfAbsent(type_, handlerList);
    }

    // 注销事件处理函数监听
    public void unregister(String type_, Method handler) {
        // 尝试获取该事件类型对应的处理函数列表，若无则忽略该次注销请求
        List<Method> handlerList = this.handlers.get(type_);

        // 如果该函数存在于列表中，则移除
        if (handlerList.contains(handler)) {
            handlerList.remove(handler);
        }

        // 如果函数列表为空，则从引擎中移除该事件类型
        if (handlerList == null || handlerList.size() == 0) {
            this.handlers.remove(type_);
        }
    }

    // 注册通用事件处理函数监听
    public void registerGeneralHandler(Method handler) {
        if (!this.generalHandlers.contains(handler)) {
            this.generalHandlers.add(handler);
        }
    }

    // 注销通用事件处理函数监听
    public void unregisterGeneralHandler(Method handler) {
        if (this.generalHandlers.contains(handler)) {
            this.generalHandlers.remove(handler);
        }
    }




    /**
     * 测试引擎
     * @param args
     */
    public static void main(String[] args) {
//        Event event = new Event(EventType.EVENT_TIMER);
//
        EventEngine eventEngine = new EventEngine();
//        Method method = new Method(eventEngine,"simpletest",Event.class);
//        //Method method1 = new Method(eventEngine,"log",Event.class);
//        Method method3 = new Method(eventEngine,"test",Event.class);
//
//
//        //eventEngine.registerGeneralHandler(simpletest);
//        List<Method> list = new ArrayList<>();
//        List<Method> list1 = new ArrayList<>();
//        list.add(method);
//        list.add(method3);
//        eventEngine.handlers.put("eTimer",list);


        //eventEngine.handlers.put("eLog",list1);
        eventEngine.start(true);
        //method.invoke(event);
    }

    //计时器通用处理方法
    public void simpletest(Event event) {
        System.out.println("处理每秒触发的计时器事件：" + LocalDateTime.now());
    }
    public void test(Event event) {
        System.out.println("测试");
    }

    //计时器通用处理方法
    public void log(Event event) {
        System.out.println("处理log事件：" + LocalDateTime.now());
    }


}
