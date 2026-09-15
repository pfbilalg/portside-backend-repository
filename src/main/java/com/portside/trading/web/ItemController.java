package com.portside.trading.web;

import com.portside.trading.domain.Item;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.web.dto.ItemDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('items')")
    public List<ItemDto> list() {
        return itemRepository.findAll().stream().map(ItemDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('items')")
    public ResponseEntity<ItemDto> create(@Valid @RequestBody ItemDto dto) {
        Item item = Item.builder().code(dto.code()).name(dto.name()).uom(dto.uom())
                .category(dto.category()).reorderLevel(dto.reorderLevel() > 0 ? dto.reorderLevel() : 400).build();
        return ResponseEntity.ok(ItemDto.of(itemRepository.save(item)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canEdit('items')")
    public ItemDto update(@PathVariable Long id, @Valid @RequestBody ItemDto dto) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        item.setCode(dto.code());
        item.setName(dto.name());
        item.setUom(dto.uom());
        item.setCategory(dto.category());
        item.setReorderLevel(dto.reorderLevel());
        return ItemDto.of(itemRepository.save(item));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.isOwner()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
