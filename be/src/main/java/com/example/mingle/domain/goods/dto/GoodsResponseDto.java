package com.example.mingle.domain.goods.dto;

import com.example.mingle.domain.goods.entity.Goods;
import com.example.mingle.domain.user.user.dto.UserSimpleDto;
import com.example.mingle.domain.user.user.entity.User;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsResponseDto {
    //상품 조회시 필요한 정보
    private Long id;
    private String itemName;
    private List<String> imgUrl;
    private String description;
    private Integer itemPrice;
    private Boolean isActive;

    //관리자용 필드
    private UserSimpleDto createdBy;

    public static GoodsResponseDto fromEntity(Goods goods) {
        User user = goods.getCreatedBy();
        return GoodsResponseDto.builder()
                .id(goods.getId())
                .itemName(goods.getItemName())
                .imgUrl(goods.getImgUrl())
                .description(goods.getDescription())
                .itemPrice(goods.getItemPrice())
                .isActive(goods.getIsActive())
                .createdBy(UserSimpleDto.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .build())
                .build();
    }
}
