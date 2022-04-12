package com.ruoyi.quartz.ctp.method;

import com.ruoyi.common.exception.ServiceException;

import java.lang.reflect.InvocationTargetException;

public class Method {
	private Object instance;
	private java.lang.reflect.Method method;
	/**
	 * @Description
	 * @author gt_vv
	 * @date 2019/11/20
	 * @param clazz  class字节码文件
	 * @param methodName   字节码中的方法名
	 * @param parameterTypes  执行 名字为methodName方法的参数
	 * @return
	 */
	public Method(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		try {
			//this.method = instance.getClass().getMethod(methodName, parameterTypes);
			//name -- 方法的名称     parameterTypes -- 参数数组   根据 clazz字节码 文件  方法名  参数数组  返回一个对应的Method
			this.method = clazz.getDeclaredMethod(methodName, parameterTypes);
			this.method.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new ServiceException("找不到method");
		}
	}

	public Method(Object instance, String methodName, Class<?>... parameterTypes) {
		this.instance = instance;
		try {
			//this.method = instance.getClass().getMethod(methodName, parameterTypes);
			//methodName 方法的简单名称    parameterTypes 参数是一个数组的Class对象识别方法的形参类型，在声明的顺序
			this.method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
			this.method.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new ServiceException("找不到method");
		}
	}

	public Object invoke(Object... args) {
		Object returnVal = null;
		try {
			System.out.println("处理成功");
			returnVal = this.method.invoke(instance, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new ServiceException("method调用失败");
		}
		return returnVal;
	}

	@Override
	public String toString() {
		return this.method.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Method) {
			Method otherMethod = (Method)obj;
			if (this.instance == null) {
				return (otherMethod.getInstance() == null) && this.method.equals(otherMethod.getMethod());
			} else {
				return this.instance.equals(otherMethod.getInstance()) && this.method.equals(otherMethod.getMethod());
			}
		} else {
			return false;
		}
	}


	///////////////////////Getter Setter////////////////////

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public java.lang.reflect.Method getMethod() {
		return method;
	}

	public void setMethod(java.lang.reflect.Method method) {
		this.method = method;
	}

	public static void main(String[] args) {

	}

}
