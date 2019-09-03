package com.practice;

import com.practice.bean.Student;
import com.practice.mapper.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMapperTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeClass
  public static void init() {
    try {
      Reader reader = Resources.getResourceAsReader("com/practice/mybatis-config.xml");
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSelectList() {
    SqlSession sqlSession = null;
    try {
      sqlSession = sqlSessionFactory.openSession();

      StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);

      List<Student> students = mapper.selectAll();

      for (int i = 0; i < students.size(); i++) {
        System.out.println(students.get(i));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (sqlSession != null) {
        sqlSession.close();
      }
    }
  }

  @Test
  public void testSelectBtweenCreatedTimeMap() {

    Map<String, Object> params = new HashMap<>();
    Calendar bTime = Calendar.getInstance();
    // month 是从0~11， 所以9月是8
    bTime.set(2018, Calendar.AUGUST, 29);
    params.put("bTime", bTime.getTime());

    Calendar eTime = Calendar.getInstance();
    eTime.set(2018,Calendar.SEPTEMBER,2);
    params.put("eTime", eTime.getTime());
    SqlSession sqlSession = null;
    try {
      sqlSession = sqlSessionFactory.openSession();

      StudentMapper studentMapper = (StudentMapper) sqlSession.getMapper(StudentMapper.class);
      List<Student> students = studentMapper.selectBetweenCreatedTime(params);
      for (int i = 0; i < students.size(); i++) {
        System.out.println(students.get(i));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (sqlSession != null) {
        sqlSession.close();
      }
    }
  }

  @Test
  public void testSelectBtweenCreatedTimeAnno() {

    Map<String, Object> params = new HashMap<>();
    Calendar bTime = Calendar.getInstance();
    // month 是从0~11， 所以9月是8
    bTime.set(2018, Calendar.AUGUST, 29);


    Calendar eTime = Calendar.getInstance();
    eTime.set(2018,Calendar.SEPTEMBER,2);

    SqlSession sqlSession = null;
    try {
      sqlSession = sqlSessionFactory.openSession();

      StudentMapper studentMapper = (StudentMapper) sqlSession.getMapper(StudentMapper.class);
      List<Student> students = studentMapper.selectBetweenCreatedTimeAnno(bTime.getTime(), eTime.getTime());
      for (int i = 0; i < students.size(); i++) {
        System.out.println(students.get(i));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (sqlSession != null) {
        sqlSession.close();
      }
    }

  }
}
