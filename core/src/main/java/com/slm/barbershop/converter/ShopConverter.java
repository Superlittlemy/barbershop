package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopResponse;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShopConverter {

    @Mapping(target = "weeklyOff", source = "request.weeklyOff", qualifiedByName = "weeklyOffListToString")
    void updateEntity(@MappingTarget Shop shop, ShopRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "weeklyOff", source = "request.weeklyOff", qualifiedByName = "weeklyOffListToString")
    Shop toEntityWithUserId(ShopRequest request, Long userId);

    @Mapping(target = "weeklyOff", source = "weeklyOff", qualifiedByName = "weeklyOffStringToList")
    ShopResponse toResponse(Shop shop);

    List<ShopResponse> toResponseList(List<Shop> shops);

    @Named("weeklyOffListToString")
    default String weeklyOffListToString(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return "0000000";
        }
        StringBuilder sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            sb.append(i < list.size() && list.get(i) != null && list.get(i) == 1 ? '1' : '0');
        }
        return sb.toString();
    }

    @Named("weeklyOffStringToList")
    default List<Integer> weeklyOffStringToList(String s) {
        List<Integer> list = new ArrayList<>(7);
        if (s == null || s.length() < 7) {
            for (int i = 0; i < 7; i++) list.add(0);
            return list;
        }
        for (int i = 0; i < 7; i++) {
            list.add(s.charAt(i) == '1' ? 1 : 0);
        }
        return list;
    }

}
