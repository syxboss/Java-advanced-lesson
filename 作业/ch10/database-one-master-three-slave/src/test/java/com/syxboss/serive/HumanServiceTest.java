package com.syxboss.serive;

import com.syxboss.entity.Human;
import com.syxboss.service.HumanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class HumanServiceTest {
    @Autowired
    private HumanService humanService;

    @Test
    public void selectHumanListTest(){
        List<Human> humanList = humanService.getHumaList();
        humanList.forEach(human -> System.out.println(human));
    }

    @Test
    public void selectHumanByIdTest(){
        Long id = 1L;
        Human human = humanService.selectHumanById(id);
        System.out.println(human);
    }

    @Test
    public void insertHumanTest(){
        Human human = new Human();
        human.setUsername("关羽");
        humanService.insertHuman(human);
        System.out.println("插入成功");
    }

    @Test
    public void UpdatetHumanTest(){
        Human human = new Human();
        human.setId(4L);
        human.setUsername("吕蒙");
        humanService.updateHuman(human);
        System.out.println("都督，我杀了关羽夺回了荆州！");
    }

    @Test
    public void deleteHumanTest(){
        Long id = 2L;
        humanService.deleteHumanById(id);
        System.out.println("不可能，我二弟天下无敌！");
    }
}
