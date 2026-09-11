package com.edumind.resource.converter;

import com.edumind.resource.entity.ResourceEntity;
import com.edumind.resource.vo.ResourceVO;
import org.springframework.stereotype.Component;

@Component
public class ResourceConverter {

    public ResourceVO toVO(ResourceEntity entity) {
        if (entity == null) {
            return null;
        }
        ResourceVO vo = new ResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setChapterId(entity.getChapterId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setFileUrl(entity.getFileUrl());
        vo.setDescription(entity.getDescription());
        return vo;
    }
}
