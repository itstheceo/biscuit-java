package com.clevercloud.biscuit.token.builder;


import com.clevercloud.biscuit.crypto.PublicKey;
import com.clevercloud.biscuit.datalog.*;
import com.clevercloud.biscuit.datalog.Check;
import com.clevercloud.biscuit.datalog.Fact;
import com.clevercloud.biscuit.datalog.Scope;
import com.clevercloud.biscuit.datalog.Rule;
import com.clevercloud.biscuit.error.Error;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Option;

import static com.clevercloud.biscuit.datalog.Check.Kind.One;
import static com.clevercloud.biscuit.token.builder.Utils.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Block {
    long index;
    int symbol_start;
    int publicKeyStart;
    SymbolTable symbols;
    String context;
    List<Fact> facts;
    List<Rule> rules;
    List<Check> checks;
    List<Scope> scopes;

    public Block(long index, SymbolTable base_symbols) {
        this.index = index;
        this.symbol_start = base_symbols.currentOffset();
        this.publicKeyStart = base_symbols.currentPublicKeyOffset();
        this.symbols = new SymbolTable(base_symbols);
        this.context = "";
        this.facts = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.checks = new ArrayList<>();
        this.scopes = new ArrayList<>();
    }

    public Block add_fact(com.clevercloud.biscuit.token.builder.Fact f) {
        this.facts.add(f.convert(this.symbols));
        return this;
    }

    public Block add_fact(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Fact>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.fact(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Fact> t = res.get();

        return add_fact(t._2);
    }

    public Block add_rule(com.clevercloud.biscuit.token.builder.Rule rule) {
        this.rules.add(rule.convert(this.symbols));
        return this;
    }

    public Block add_rule(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Rule>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.rule(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Rule> t = res.get();

        return add_rule(t._2);
    }

    public Block add_check(com.clevercloud.biscuit.token.builder.Check check) {
        this.checks.add(check.convert(this.symbols));
        return this;
    }

    public Block add_check(String s) throws Error.Parser {
        Either<com.clevercloud.biscuit.token.builder.parser.Error, Tuple2<String, com.clevercloud.biscuit.token.builder.Check>> res =
                com.clevercloud.biscuit.token.builder.parser.Parser.check(s);

        if (res.isLeft()) {
            throw new Error.Parser(res.getLeft());
        }

        Tuple2<String, com.clevercloud.biscuit.token.builder.Check> t = res.get();

        return add_check(t._2);
    }

    public Block add_scope(com.clevercloud.biscuit.token.builder.Scope scope) {
        this.scopes.add(scope.convert(this.symbols));
        return this;
    }

    public Block set_context(String context) {
        this.context = context;
        return this;
    }

    public com.clevercloud.biscuit.token.Block build() {
        SymbolTable symbols = new SymbolTable();

        for (int i = this.symbol_start; i < this.symbols.symbols.size(); i++) {
            symbols.add(this.symbols.symbols.get(i));
        }

        List<PublicKey> publicKeys = new ArrayList<>();
        for (int i = this.publicKeyStart; i < this.symbols.currentPublicKeyOffset(); i++) {
            publicKeys.add(this.symbols.publicKeys().get(i));
        }

        SchemaVersion schemaVersion = new SchemaVersion(this.facts, this.rules, this.checks, this.scopes);

        return new com.clevercloud.biscuit.token.Block(symbols, this.context, this.facts, this.rules, this.checks,
                this.scopes, publicKeys, Option.none(), schemaVersion.version());
    }

    public Block check_right(String right) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();
        queries.add(rule(
                "check_right",
                Arrays.asList(s(right)),
                Arrays.asList(
                        pred("resource", Arrays.asList(var("resource"))),
                        pred("operation", Arrays.asList(s(right))),
                        pred("right", Arrays.asList(var("resource"), s(right)))
                )
        ));
        return this.add_check(new com.clevercloud.biscuit.token.builder.Check(One, queries));
    }

    public Block resource_prefix(String prefix) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "prefix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Prefix, new Expression.Value(var("resource")),
                        new Expression.Value(string(prefix))))
        ));
        return this.add_check(new com.clevercloud.biscuit.token.builder.Check(One, queries));
    }

    public Block resource_suffix(String suffix) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "suffix",
                Arrays.asList(var("resource")),
                Arrays.asList(pred("resource", Arrays.asList(var("resource")))),
                Arrays.asList(new Expression.Binary(Expression.Op.Suffix, new Expression.Value(var("resource")),
                        new Expression.Value(string(suffix))))
        ));
        return this.add_check(new com.clevercloud.biscuit.token.builder.Check(One, queries));
    }

    public Block expiration_date(Date d) {
        ArrayList<com.clevercloud.biscuit.token.builder.Rule> queries = new ArrayList<>();

        queries.add(constrained_rule(
                "expiration",
                Arrays.asList(var("date")),
                Arrays.asList(pred("time", Arrays.asList(var("date")))),
                Arrays.asList(new Expression.Binary(Expression.Op.LessOrEqual, new Expression.Value(var("date")),
                        new Expression.Value(date(d))))
        ));
        return this.add_check(new com.clevercloud.biscuit.token.builder.Check(One, queries));
    }
}
