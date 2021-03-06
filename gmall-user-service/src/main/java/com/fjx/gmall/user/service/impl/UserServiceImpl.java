package com.fjx.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.UmsMember;
import com.fjx.gmall.bean.UmsMemberReceiveAddress;
import com.fjx.gmall.service.UserService;
import com.fjx.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.fjx.gmall.user.mapper.UserMapper;
import com.fjx.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> umsMemberList = userMapper.selectAll();//userMapper.selectAllUser();

        return umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> list = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return list;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
    /*    1、redis连接不上
          2、redis中没有用户信息
          3、用户不存在、查询数据库要使用分布式锁
    */
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String userStr = jedis.get("user:" + umsMember.getUsername() + umsMember.getPassword() + ":password");
                if (StringUtils.isNotBlank(userStr)) {
                    UmsMember memberCheck = JSON.parseObject(userStr, UmsMember.class);
                    String password = null;
                    password = memberCheck.getPassword();
                    if (password != null && password.equals(umsMember.getPassword())) {
                        //密码正确
                        return memberCheck;
                    }
                }


            }
            //redis连接失败
            RLock lock = null;
            try {
                lock = redissonClient.getLock("UmsMemberLock");
                lock.lock();
                UmsMember memberInDB = loginFromDB(umsMember);
                if (memberInDB != null) {
                    jedis.setex("user:" + umsMember.getUsername() + umsMember.getPassword() + ":password",
                            60 * 60 * 24,
                            JSON.toJSONString(memberInDB));
                    return memberInDB;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = null;
        jedis = redisUtil.getJedis();
        try {
            jedis.setex("user:" + memberId + ":token", 60 * 60 * 24, token);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Override
    public UmsMember addOAuthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }

    @Override
    public UmsMember checkAuthUser(UmsMember user) {
        Example example = new Example(UmsMember.class);
        example.createCriteria().andEqualTo("sourceUid", user.getSourceUid());
        UmsMember umsMember = userMapper.selectOneByExample(example);
        return umsMember;
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receieveAddressId) {
        UmsMemberReceiveAddress condition = new UmsMemberReceiveAddress();
        condition.setId(receieveAddressId);
        return umsMemberReceiveAddressMapper.selectOne(condition);
    }

    private UmsMember loginFromDB(UmsMember umsMember) {

        List<UmsMember> res = userMapper.select(umsMember);
        if (res != null && res.size() == 1) {
            return res.get(0);
        }

        return null;
    }

}
