package com.practice;

import com.practice.bean.Auther;
import com.practice.bean.BlogCustom;
import com.practice.bean.BlogBo;
import com.practice.mapper.BlogMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringBuilder;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringStyle;

import java.io.IOException;
import java.io.Reader;

public class BlogMapperTest {

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
  public void testSelectCustomById() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogCustom blogCustom = blogMapper.selectCutomById(1);
    System.out.println(ToStringBuilder.reflectionToString(blogCustom, ToStringStyle.MULTI_LINE_STYLE));
  }

  @Test
  public void testSelectBoById() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogBo blogBO = blogMapper.selectBoById(1);
    System.out.println(ToStringBuilder.reflectionToString(blogBO, ToStringStyle.MULTI_LINE_STYLE));
  }

  /**
   *  resultMap 方式一测试
   */
  @Test
  public void testSelectCustomByIdMap() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogCustom blogCustom = blogMapper.selectCutomByIdMap(1);
    System.out.println(ToStringBuilder.reflectionToString(blogCustom, ToStringStyle.MULTI_LINE_STYLE));
  }

  /**
   *  resultMap + association 方式测试
   */
  @Test
  public void testSelectCustomByIdAMap() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogCustom blogCustom = blogMapper.selectCutomByIdAMap(1);
    System.out.println(ToStringBuilder.reflectionToString(blogCustom, ToStringStyle.MULTI_LINE_STYLE));
  }

  /**
   *  resultMap + association 嵌套查询方式测试
   */
  @Test
  public void testSelectBlogAndAuthorByIdSelect() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogCustom blogCustom = blogMapper.selectBlogAndAuthorByIdSelect(2);
    System.out.println(ToStringBuilder.reflectionToString(blogCustom, ToStringStyle.MULTI_LINE_STYLE));

    Assert.assertNotNull(blogCustom);
    Assert.assertNotNull(blogCustom.getAuther());

  }

  /**
   *  resultMap + association 嵌套查询方式测试懒加载测试
   */
  @Test
  public void testSelectBlogAndAuthorByIdSelectByLazy() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    BlogMapper blogMapper = sqlSession.getMapper(BlogMapper.class);
    BlogCustom blogCustom = blogMapper.selectBlogAndAuthorByIdSelectByLazy(2);
    Assert.assertNotNull(blogCustom);
    System.out.println("开始使用author对象");
    Auther auther = blogCustom.getAuther();
    Assert.assertNotNull(auther.getUsername());
  }
}
