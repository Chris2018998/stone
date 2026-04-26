package org.stone.test.beeop.objects.factory;

import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.book.TextBook;

public class TextBookFactory3 extends TextBookFactory {
    public TextBookFactory3() {
        super();
    }

    public TextBookFactory3(String title, String author) {
        super(title, author);
    }

    @Override
    public Book create(String key) throws Exception {
        TextBook book = new TextBook(this.title, this.author);
        if (this.callException != null) book.setFailException(callException);
        return book;
    }
}
