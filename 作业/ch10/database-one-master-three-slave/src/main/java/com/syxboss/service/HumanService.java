package com.syxboss.service;

import com.syxboss.entity.Human;

import java.util.List;

public interface HumanService {
    List<Human> getHumaList();
    Human selectHumanById(Long id);
    void insertHuman(Human human);
    void updateHuman(Human human);
    void deleteHumanById(Long id);
}
