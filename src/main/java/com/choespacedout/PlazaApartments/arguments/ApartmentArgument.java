package com.choespacedout.PlazaApartments.arguments;

import com.choespacedout.PlazaApartments.ApartmentSetupCache;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@NullMarked
public class ApartmentArgument implements CustomArgumentType<String,String> {

    ApartmentSetupCache apartmentSetupCache;

    public ApartmentArgument(ApartmentSetupCache newApartmentSetupCache) {
        apartmentSetupCache = newApartmentSetupCache;
    }

    private static final DynamicCommandExceptionType ERROR_NO_APARTMENT = new DynamicCommandExceptionType(name -> {
        return MessageComponentSerializer.message().serialize(Component.text("No apartment was found"));
    });

    private static final SimpleCommandExceptionType ERROR_BAD_SOURCE = new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("The source needs to be a CommandSourceStack")));


    @Override
    public String parse(StringReader stringReader) {
        throw new UnsupportedOperationException("This method will never be called.");
    }

    @Override
    public <S> String parse(StringReader reader, S source) throws CommandSyntaxException {

        if (!(source instanceof CommandSourceStack stack)) {
            throw ERROR_BAD_SOURCE.create();
        }

        final List<String> apartments = apartmentSetupCache.getAllApartmentSetups().keySet().stream().map(String::toLowerCase).toList();
        final String apartmentName = getNativeType().parse(reader);

        if (!apartments.contains(apartmentName.toLowerCase())) {
            throw ERROR_NO_APARTMENT.create(apartmentName);
        }

        return apartmentName;

    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {

        final List<String> apartments = apartmentSetupCache.getAllApartmentSetups().keySet().stream().toList();

        for (int i = 0; i < apartments.size(); i++) {
            String apartmentName = apartments.get(i);

            if (apartmentName.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(apartmentName);
            }
        }

        return builder.buildFuture();

    }
}