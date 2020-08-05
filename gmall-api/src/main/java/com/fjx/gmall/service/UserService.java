package com.fjx.gmall.service;


import com.fjx.gmall.bean.UmsMember;
import com.fjx.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
