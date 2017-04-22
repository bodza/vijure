package vijure;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class VimA
{
    public/*private*/ static final int char_u(byte b)
    {
        return (b < 0) ? b + 0x100 : b;
    }

    public/*private*/ static final long int_u(int i)
    {
        return (i < 0) ? i + 0x100000000L : i;
    }

    /* ----------------------------------------------------------------------- */

    /*private*/ static final byte NUL = '\000';

    /*private*/ static final Charset UTF8 = Charset.forName("UTF-8");

    public/*private*/ static Bytes u8(String s)
    {
        ByteBuffer bb = UTF8.encode(s);
        int n = bb.remaining();
        byte[] ba = new byte[n + 1];
        bb.get(ba, 0, n);
        return new Bytes(ba);
    }

    public/*private*/ static final class Bytes
    {
        byte[]      array;
        int         index;

        public/*private*/ Bytes(int length)
        {
            array = new byte[length];
        }

        public/*private*/ Bytes(byte[] array)
        {
            this.array = array;
        }

        public/*private*/ Bytes(byte[] array, int index)
        {
            this.array = array;
            this.index = index;
        }

        public/*private*/ int size()
        {
            return array.length - index;
        }

        public/*private*/ byte at(int i)
        {
            return array[index + i];
        }

        public/*private*/ Bytes be(int i, byte b)
        {
            array[index + i] = b;

            return this;
        }

        public/*private*/ final Bytes be(int i, int c)
        {
            be(i, (byte)c);	// %%

            return this;
        }

        public/*private*/ ByteBuffer buf()
        {
            return ByteBuffer.wrap(array, index, array.length - index);
        }

        public/*private*/ Bytes plus(int i)
        {
            return new Bytes(array, index + i);
        }

        public/*private*/ Bytes minus(int i)
        {
            return new Bytes(array, index - i);
        }
    }

    public/*private*/ static boolean BEQ(Bytes s1, Bytes s0)
    {
        return (s1 == s0 || (s1 != null && s0 != null && s1.array == s0.array && s1.index == s0.index));
    }

    public/*private*/ static final boolean BNE(Bytes s1, Bytes s0)
    {
        return !BEQ(s1, s0);
    }

    public/*private*/ static int BDIFF(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BDIFF array mismatch");

        return s1.index - s0.index;
    }

    public/*private*/ static boolean BLT(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BLT array mismatch");

        return (s1.index < s0.index);
    }

    public/*private*/ static boolean BLE(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BLE array mismatch");

        return (s1.index <= s0.index);
    }

    public/*private*/ static int asc_toupper(int c)
    {
        return (c < 'a' || 'z' < c) ? c : c - ('a' - 'A');
    }

    public/*private*/ static int asc_tolower(int c)
    {
        return (c < 'A' || 'Z' < c) ? c : c + ('a' - 'A');
    }

    public/*private*/ static Bytes MEMCHR(Bytes p, byte b, int n)
    {
        for (int i = 0; i < n; i++)
            if (p.at(i) == b)
                return p.plus(i);

        return null;
    }

    public/*private*/ static int MEMCMP(Bytes p1, Bytes p2, int n)
    {
        for (int i = 0; i < n; i++)
        {
            int cmp = p1.at(i) - p2.at(i);
            if (cmp != 0)
                return cmp;
        }

        return 0;
    }

    public/*private*/ static void ACOPY(byte[] d, int di, byte[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    public/*private*/ static void ACOPY(short[] d, int di, short[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    public/*private*/ static void ACOPY(int[] d, int di, int[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    public/*private*/ static void ACOPY(Object[] d, int di, Object[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    public/*private*/ static void BCOPY(Bytes d, int di, Bytes s, int si, int n)
    {
        System.arraycopy(s.array, s.index + si, d.array, d.index + di, n);
    }

    public/*private*/ static void BCOPY(Bytes d, Bytes s, int n)
    {
        BCOPY(d, 0, s, 0, n);
    }

    public/*private*/ static void AFILL(boolean[] a, boolean b)
    {
        Arrays.fill(a, b);
    }

    public/*private*/ static void AFILL(int[] a, int i)
    {
        Arrays.fill(a, i);
    }

    public/*private*/ static void AFILL(int[] a, int ai, int i, int n)
    {
        Arrays.fill(a, ai, ai + n, i);
    }

    public/*private*/ static void BFILL(Bytes p, int pi, byte b, int n)
    {
        Arrays.fill(p.array, p.index + pi, p.index + pi + n, b);
    }

    public/*private*/ static int STRCASECMP(Bytes s1, Bytes s2)
    {
        if (BNE(s1, s2))
        {
            byte b1;
            do
            {
                int cmp = asc_tolower(b1 = (s1 = s1.plus(1)).at(-1)) - asc_tolower((s2 = s2.plus(1)).at(-1));
                if (cmp != 0)
                    return cmp;
            }
            while (b1 != NUL);
        }

        return 0;
    }

    public/*private*/ static Bytes STRCAT(Bytes d, Bytes s)
    {
        Bytes p = d;
        byte b;

        do
        {
            b = (p = p.plus(1)).at(-1);
        }
        while (b != NUL);

        p = p.minus(2);

        do
        {
            b = (s = s.plus(1)).at(-1);
            (p = p.plus(1)).be(0, b);
        }
        while (b != NUL);

        return d;
    }

    public/*private*/ static Bytes STRCHR(Bytes s, byte b)
    {
        for ( ; ; s = s.plus(1))
        {
            if (s.at(0) == b)
                return s;
            if (s.at(0) == NUL)
                return null;
        }
    }

    public/*private*/ static int STRCMP(Bytes s1, Bytes s2)
    {
        byte b1, b2;

        do
        {
            b1 = (s1 = s1.plus(1)).at(-1);
            b2 = (s2 = s2.plus(1)).at(-1);
        }
        while (b1 != NUL && b1 == b2);

        return b1 - b2;
    }

    public/*private*/ static void STRCPY(Bytes d, Bytes s)
    {
        byte b;
        do
        {
            b = (s = s.plus(1)).at(-1);
            (d = d.plus(1)).be(-1, b);
        }
        while (b != NUL);
    }

    public/*private*/ static final int STRLEN(Bytes s, int i)
    {
        return STRLEN(s.plus(i));
    }

    public/*private*/ static int STRLEN(Bytes s)
    {
        for (Bytes p = s; ; p = p.plus(1))
            if (p.at(0) == NUL)
                return BDIFF(p, s);
    }

    public/*private*/ static int STRNCASECMP(Bytes s1, Bytes s2, int n)
    {
        if (BNE(s1, s2) && 0 < n)
        {
            byte b1;
            do
            {
                int cmp = asc_tolower(b1 = (s1 = s1.plus(1)).at(-1)) - asc_tolower((s2 = s2.plus(1)).at(-1));
                if (cmp != 0)
                    return cmp;
            }
            while (b1 != NUL && 0 < --n);
        }

        return 0;
    }

    public/*private*/ static Bytes STRNCAT(Bytes d, Bytes s, int n)
    {
        Bytes p = d;
        byte b;

        do
        {
            b = (p = p.plus(1)).at(-1);
        }
        while (b != NUL);

        p = p.minus(2);

        for ( ; 0 < n; --n)
        {
            b = (s = s.plus(1)).at(-1);
            (p = p.plus(1)).be(0, b);
            if (b == NUL)
                return d;
        }

        if (b != NUL)
            (p = p.plus(1)).be(0, NUL);

        return d;
    }

    public/*private*/ static int STRNCMP(Bytes s1, Bytes s2, int n)
    {
        for ( ; 0 < n; --n)
        {
            byte b1 = (s1 = s1.plus(1)).at(-1), b2 = (s2 = s2.plus(1)).at(-1);

            if (b1 == NUL || b1 != b2)
                return b1 - b2;
        }

        return 0;
    }

    public/*private*/ static Bytes STRNCPY(Bytes d, Bytes s, int n)
    {
        if (0 < n)
        {
            Bytes p = d;
            byte b;

            p = p.minus(1);

            do
            {
                b = (s = s.plus(1)).at(-1);
                (p = p.plus(1)).be(0, b);
                if (--n == 0)
                    return d;
            }
            while (b != NUL);

            do
            {
                (p = p.plus(1)).be(0, NUL);
            }
            while (0 < --n);
        }

        return d;
    }

    public/*private*/ static Bytes STRPBRK(Bytes s, Bytes accept)
    {
        for ( ; s.at(0) != NUL; s = s.plus(1))
            for (Bytes a = accept; a.at(0) != NUL; a = a.plus(1))
                if (a.at(0) == s.at(0))
                    return s;

        return null;
    }

    /*private*/ static final int SIZE_MAX = 0xffffffff;

    /*
     * We use the Two-Way string matching algorithm, which guarantees linear
     * complexity with constant space.  Additionally, for long needles,
     * we also use a bad character shift table similar to the Boyer-Moore
     * algorithm to achieve improved (potentially sub-linear) performance.
     *
     * See http://www-igm.univ-mlv.fr/~lecroq/string/node26.html#SECTION00260
     * and http://en.wikipedia.org/wiki/Boyer-Moore_string_search_algorithm
     */

    /*private*/ static int critical_factorization(Bytes needle, int needle_len, int[] period)
    {
        int max_suffix = SIZE_MAX;
        int j = 0;
        int p = 1;
        int k = p;
        while (j + k < needle_len)
        {
            byte a = needle.at(j + k);
            byte b = needle.at(max_suffix + k);
            if (a < b)
            {
                j += k;
                k = 1;
                p = j - max_suffix;
            }
            else if (a == b)
            {
                if (k != p)
                    k++;
                else
                {
                    j += p;
                    k = 1;
                }
            }
            else
            {
                max_suffix = j++;
                k = p = 1;
            }
        }
        period[0] = p;

        int max_suffix_rev = SIZE_MAX;
        j = 0;
        k = p = 1;
        while (j + k < needle_len)
        {
            byte a = needle.at(j + k);
            byte b = needle.at(max_suffix_rev + k);
            if (b < a)
            {
                j += k;
                k = 1;
                p = j - max_suffix_rev;
            }
            else if (a == b)
            {
                if (k != p)
                    k++;
                else
                {
                    j += p;
                    k = 1;
                }
            }
            else
            {
                max_suffix_rev = j++;
                k = p = 1;
            }
        }

        if (max_suffix_rev + 1 < max_suffix + 1)
            return max_suffix + 1;

        period[0] = p;
        return max_suffix_rev + 1;
    }

    /*private*/ static Bytes two_way_short_needle(Bytes haystack, int haystack_len, Bytes needle, int needle_len)
    {
        int[] period = new int[1];
        int suffix = critical_factorization(needle, needle_len, period);

        if (MEMCMP(needle, needle.plus(period[0]), suffix) == 0)
        {
            int memory = 0;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int i = (suffix < memory) ? memory : suffix;
                while (i < needle_len && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len <= i)
                {
                    i = suffix - 1;
                    while (memory < i + 1 && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i + 1 < memory + 1)
                        return haystack.plus(j);

                    j += period[0];
                    memory = needle_len - period[0];
                }
                else
                {
                    j += i - suffix + 1;
                    memory = 0;
                }
            }
        }
        else
        {
            period[0] = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int i = suffix;
                while (i < needle_len && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len <= i)
                {
                    i = suffix - 1;
                    while (i != SIZE_MAX && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i == SIZE_MAX)
                        return haystack.plus(j);
                    j += period[0];
                }
                else
                    j += i - suffix + 1;
            }
        }

        return null;
    }

    /*private*/ static Bytes two_way_long_needle(Bytes haystack, int haystack_len, Bytes needle, int needle_len)
    {
        int[] shift_table = new int[1 << 8];

        int[] period = new int[1];
        int suffix = critical_factorization(needle, needle_len, period);

        for (int i = 0; i < (1 << 8); i++)
            shift_table[i] = needle_len;
        for (int i = 0; i < needle_len; i++)
            shift_table[char_u(needle.at(i))] = needle_len - i - 1;

        if (MEMCMP(needle, needle.plus(period[0]), suffix) == 0)
        {
            int memory = 0;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int shift = shift_table[char_u(haystack.at(j + needle_len - 1))];
                if (0 < shift)
                {
                    if (memory != 0 && shift < period[0])
                    {
                        shift = needle_len - period[0];
                        memory = 0;
                    }
                    j += shift;
                    continue;
                }
                int i = (suffix < memory) ? memory : suffix;
                while (i < needle_len - 1 && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len - 1 <= i)
                {
                    i = suffix - 1;
                    while (memory < i + 1 && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i + 1 < memory + 1)
                        return haystack.plus(j);

                    j += period[0];
                    memory = needle_len - period[0];
                }
                else
                {
                    j += i - suffix + 1;
                    memory = 0;
                }
            }
        }
        else
        {
            period[0] = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int shift = shift_table[char_u(haystack.at(j + needle_len - 1))];
                if (0 < shift)
                {
                    j += shift;
                    continue;
                }
                int i = suffix;
                while (i < needle_len - 1 && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len - 1 <= i)
                {
                    i = suffix - 1;
                    while (i != SIZE_MAX && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i == SIZE_MAX)
                        return haystack.plus(j);
                    j += period[0];
                }
                else
                    j += i - suffix + 1;
            }
        }

        return null;
    }

    public/*private*/ static Bytes STRSTR(Bytes haystack_start, Bytes needle_start)
    {
        Bytes haystack = haystack_start;
        Bytes needle = needle_start;

        boolean ok = true;
        while (haystack.at(0) != NUL && needle.at(0) != NUL)
            ok &= ((haystack = haystack.plus(1)).at(-1) == (needle = needle.plus(1)).at(-1));
        if (needle.at(0) != NUL)
            return null;
        if (ok)
            return haystack_start;

        int needle_len = BDIFF(needle, needle_start);
        haystack = STRCHR(haystack_start.plus(1), needle_start.at(0));
        if (haystack == null || needle_len == 1)
            return haystack;

        needle = needle.minus(needle_len);
        int haystack_len = (BLT(haystack_start.plus(needle_len), haystack)) ? 1 : BDIFF(haystack_start.plus(needle_len), haystack);

        if (needle_len < 32)
            return two_way_short_needle(haystack, haystack_len, needle, needle_len);

        return two_way_long_needle(haystack, haystack_len, needle, needle_len);
    }
}
