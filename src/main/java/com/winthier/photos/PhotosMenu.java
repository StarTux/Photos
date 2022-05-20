package com.winthier.photos;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.money.Money;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.photo.Photo;
import com.winthier.photos.util.Gui;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static com.cavetale.mytems.item.photo.Photo.SEPIA;
import static com.cavetale.mytems.util.Items.text;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static org.bukkit.Sound.*;
import static org.bukkit.SoundCategory.*;

/**
 * An instance of this class contains all information for an opened
 * PhotosMenu, which is a chest GUI.
 *
 * To open a PhotosMenu, call `new PhotosMenu(plugin, list).open(player)`.
 */
@RequiredArgsConstructor
public final class PhotosMenu {
    private final PhotosPlugin plugin;
    private final List<PhotoRuntime> photos;
    int pageIndex = 0;

    /**
     * Open the menu for a player.  This will create an inventory
     * fitting for the given player's Photo collection, and populate
     * it with clickable items.
     */
    public void open(final Player player) {
        final int size = 6 * 9;
        final int pageSize = 5 * 9;
        final int pageCount = (photos.size() - 1) / pageSize + 1;
        Gui gui = new Gui(plugin).size(size);
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, color(SEPIA))
            .layer(GuiOverlay.TOP_BAR, WHITE)
            .title(join(noSeparators(),
                        (pageCount > 1
                         ? text((pageIndex + 1) + "/" + pageCount + " ", BLACK)
                         : empty()),
                        text("Photos Menu", color(SEPIA))));
        for (int i = 0; i < pageSize; i += 1) {
            final int invIndex = 9;
            final int listIndex = pageIndex * pageSize + i;
            if (listIndex >= photos.size()) break;
            PhotoRuntime photo = photos.get(listIndex);
            ItemStack icon = Photo.createItemStack(photo.getRow().getId());
            icon.editMeta(meta -> {
                    text(meta, List.of(text(photo.getRow().getName(), color(photo.getRow().getColor())),
                                       join(noSeparators(),
                                            text(tiny("shift-click "), GREEN),
                                            text("Buy a copy", GRAY)),
                                       join(noSeparators(),
                                            text(tiny("price "), GRAY),
                                            text(Money.format(plugin.getCopyPrice()), GOLD))));
                });
            gui.setItem(invIndex, icon, click -> {
                    if (click.isShiftClick()) {
                        if (buyCopy(player, photo)) {
                            purchase(player);
                        } else {
                            fail(player);
                        }
                    } else if (click.isLeftClick()) {
                        buyCopyInfo(player, photo);
                        click(player);
                    }
                });
        }
        // Buy new photo
        ItemStack blankIcon = Mytems.PLUS_BUTTON
            .createIcon(List.of(text("New Photo", color(SEPIA)),
                                join(noSeparators(),
                                     text(tiny("shift-click "), GREEN),
                                     text("Buy a new blank photo", GRAY)),
                                join(noSeparators(),
                                     text(tiny("price "), GRAY),
                                     text(Money.format(plugin.getPhotoPrice()), GOLD))));
        gui.setItem(4, blankIcon, click -> {
                if (click.isLeftClick()) {
                    if (buyPhoto(player)) {
                        purchase(player);
                        player.closeInventory();
                    } else {
                        fail(player);
                    }
                }
            });
        // Page controls
        if (pageIndex > 0) {
            ItemStack prevIcon = Mytems.ARROW_LEFT.createIcon(List.of(text("Page " + pageIndex, GRAY)));
            gui.setItem(0, prevIcon, click -> {
                    pageIndex -= 1;
                    open(player);
                    pageFlip(player);
                });
        }
        if (pageIndex < pageCount - 1) {
            ItemStack nextIcon = Mytems.ARROW_RIGHT.createIcon(List.of(text("Page " + (pageIndex + 2), GRAY)));
            gui.setItem(8, nextIcon, click -> {
                    pageIndex += 1;
                    open(player);
                    pageFlip(player);
                });
        }
        gui.title(builder.build());
        gui.open(player);
    }

    /**
     * Copies of Photos are made via shift and left click.
     */
    private boolean buyCopy(Player player, PhotoRuntime photo) {
        double price = plugin.getCopyPrice();
        // Purchase
        if (!Money.take(player.getUniqueId(), price, plugin, "Make photo copy")) {
            player.sendMessage(text("You don't have " + Money.format(price) + "!", RED));
            return false;
        }
        player.sendMessage(join(noSeparators(),
                                text("Made one copy of "),
                                text(photo.getRow().getName(), null, BOLD),
                                text(" for "),
                                text(Money.format(price), GOLD))
                           .color(color(SEPIA)));
        ItemStack item = Photo.createItemStack(photo.getRow().getId());
        for (ItemStack drop: player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            player.sendMessage(text("Careful! Your inventory is full. The copy was dropped.", RED));
        }
        return false;
    }

    private void buyCopyInfo(Player player, PhotoRuntime photo) {
        double price = plugin.getCopyPrice();
        player.sendMessage(join(noSeparators(),
                                text("Shift click", GREEN),
                                text(" to make a copy of "),
                                text(photo.getRow().getName(), color(photo.getRow().getColor())),
                                text(" for "),
                                text(Money.format(price), GOLD))
                           .color(color(SEPIA)));
    }

    private boolean buyPhoto(Player player) {
        double price = plugin.getPhotoPrice();
        if (!Money.take(player.getUniqueId(), price, plugin, "Buy new photo")) {
            player.sendMessage(text("You don't have " + Money.format(price) + "!", RED));
            return false;
        }
        player.sendMessage(join(noSeparators(),
                                text("Purchased a new Photo for "),
                                text(Money.format(price), GOLD),
                                text("."))
                           .color(color(SEPIA)));
        PhotoRuntime photo = plugin.getPhotos().create(player.getUniqueId(), "Photo " + (photos.size() + 1),
                                                       PhotoColor.random().color.asRGB());
        if (photo == null) {
            plugin.getLogger().warning("Could not create Photo for " + player.getName() + "!");
            player.sendMessage(text("Something went wrong during Photo creation."
                                    + " Please contact an administrator.", RED));
            Money.give(player.getUniqueId(), price, plugin, "Photo Refund");
            return false;
        }
        ItemStack item = Photo.createItemStack(photo.getRow().getId());
        for (ItemStack drop : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), drop).setPickupDelay(0);
            player.sendMessage(text("Careful! Your inventory is full. The Photo was dropped.", RED));
        }
        return true;
    }

    private static void purchase(Player player) {
        player.playSound(player.getLocation(), ITEM_ARMOR_EQUIP_LEATHER, MASTER, 0.5f, 0.75f);
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 1.0f);
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), UI_BUTTON_CLICK, MASTER, 0.5f, 0.5f);
    }

    private static void pageFlip(Player player) {
        player.playSound(player.getLocation(), ITEM_BOOK_PAGE_TURN, MASTER, 0.5f, 1.0f);
    }
}
