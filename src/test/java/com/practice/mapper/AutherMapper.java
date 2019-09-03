package com.practice.mapper;

import com.practice.bean.Auther;

public interface AutherMapper {

  /**
   * 嵌套查询使用的方法
   * @param id
   * @return
   */
  Auther selectById(Integer id);
}
