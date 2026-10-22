package com.neueda.leap.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AnalyticsMapper {

    /** Fold newly filled fills into the analytics daily facts; returns how many were projected (see V28). */
    @Select("SELECT analytics.project_new_fills()")
    int projectNewFills();
}
