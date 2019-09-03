package com.practice.mapper;

import com.practice.bean.Student;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface StudentMapper {
  /**
   * 获取全部的学生
   * @return
   */
  List<Student> selectAll();

  /**
   * 获取一段时间内的用户
   * @param params
   * @return
   */
  List<Student> selectBetweenCreatedTime(Map<String, Object> params);

  /**
   * 获取指定时间内的对象
   * @param pbTime 开始时间
   * @param peTime 结束时间
   * @return
   */
  List<Student> selectBetweenCreatedTimeAnno(@Param("bTime") Date pbTime, @Param("eTime")Date peTime);
}
