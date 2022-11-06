package com.syxboss.service.impl;

import com.syxboss.dao.HumanMapper;
import com.syxboss.entity.Human;
import com.syxboss.service.HumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HumanServiceImpl implements HumanService{
    @Autowired
    private HumanMapper humanMapper ;

    @Override
    public List<Human> getHumaList() {
        return humanMapper.selectList(null);
    }

    @Override
    public Human selectHumanById(Long id) {
        return humanMapper.selectById(id);
    }

    @Override
    public void insertHuman(Human human) {
        humanMapper.insert(human);
    }

    @Override
    public void updateHuman(Human human) {
        humanMapper.updateById(human);
    }

    @Override
    public void deleteHumanById(Long id) {
        humanMapper.deleteById(id);
    }
}
