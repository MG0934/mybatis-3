package com.practice.mapper;

import com.practice.bean.Blog;
import com.practice.bean.BlogBo;
import com.practice.bean.BlogCustom;

public interface BlogMapper {
  BlogBo selectBoById(int id);
  /**
   * 根据博客的 id 获取博客及作者的信息
   * @param id
   * @return
   */
  BlogCustom selectCutomById(int id);

  /**
   * 根据博客的 id 获取博客及作者的信息, resultMap 方式
   * @param id
   * @return
   */
  BlogCustom selectCutomByIdMap(int id);

  /**
   * 根据博客的 id 获取博客及作者的信息, resultMap + association方式
   * @param id
   * @return
   */
  BlogCustom selectCutomByIdAMap(int id);

  /**
   * 根据博客的 id 获取博客及作者的信息, resultMap + association嵌套方式
   * @param id
   * @return
   */
  BlogCustom selectBlogAndAuthorByIdSelect(int id);

  BlogCustom selectBlogAndAuthorByIdSelectByLazy(int id);
}
