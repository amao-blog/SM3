import java.io.UnsupportedEncodingException;


public class SM3 {

	private String charset = "ISO-8859-1";

	// Ҫ��ϣ���ַ���
	private String message = "abc";

	// ������ַ���
	private String PaddingMessage;

	// ��ȡ����T0��T1
	private int T(int j) {
		if (j <= 15) {
			return 0x79cc4519;
		} else {
			return 0x7a879d8a;
		}
	}

	// �������� FF
	private int FF(int X, int Y, int Z, int j) {
		int result = 0;
		if (j >= 0 && j <= 15) {
			result = X ^ Y ^ Z;
		} else if (j >= 16 && j <= 63) {
			result = (X & Y) | (X & Z) | (Y & Z);
		}
		return result;
	}

	// ��������GG
	private int GG(int X, int Y, int Z, int j) {
		int result = 0;
		if (j >= 0 && j <= 15) {
			result = X ^ Y ^ Z;
		} else if (j >= 16 && j <= 63) {
			result = (X & Y) | (~X & Z);
		} else {

			System.out.println("Wrong Param J in GG: " + j);
		}
		return result;
	}

	// �û�����P0
	private int P0(int X) {
		return X ^ (CircleLeftShift(X, 9)) ^ CircleLeftShift(X, 17);
	}

	// �û�����P1
	private int P1(int X) {
		return X ^ (CircleLeftShift(X, 15)) ^ CircleLeftShift(X, 23);
	}

	// ѭ������
	private static int CircleLeftShift(int x, int N) {
		return (x << N) | (x >>> (32 - N));
	}

	// ����ַ���
	private void Padding() {
		// ��ȡԭʼ��Ϣ������λ����
		long strLength = this.message.length() * 8;
		// ���Ĳ���
		String padding = "";

		// ����������512�ֽڵĲ�����Ϣ����
		int left = (int) (strLength - (strLength / 512) * 512);

		// ��Ϣ���ȳ�ȥ������512λ��ʣ�����Ϣ���ȣ��������447λ����Ҫ�ٲ�һ��512λ
		int k = 0; // Ҫ���0��λ��
		if (left <= 447) {
			k = 447 - left; // Ҫ�����ô���0
		} else {
			k = left + 512 - 65;
		}
		// System.out.println("���" + k +"��0");
		// ��ʼ���
		byte a[] = {(byte)0x80};
		try {
			padding += new String(a, charset);
		} catch (Exception e) {
			System.out.println("Error:" + e.getMessage());
		}
		for (int ch_k = k / 8; ch_k > 0; ch_k--) {
			padding += (char) 0x00;
		}
		// �������64λ��l��ʾ��Ϣ�ĳ���
		/*
		 * byte[] strLengthByte = longToByte8(strLength); for(int i = 0; i < 8;
		 * i++){ padding += (char) strLengthByte[i]; }
		 */
		padding += longToString(strLength);
		// ������ַ���
		this.PaddingMessage = this.message + padding;
		 
	}

	public static void main(String args[]) {
		System.out.println("�Ӵ�ֵ��"+hash("abc"));
	}

	public static String hash(String message) {
		String hash = "";
		int result[];
		SM3 obj = new SM3();
		obj.message = message;
		// 1.�����Ϣ
		obj.Padding();
		// 2.����ѹ��
		result = obj.IterativeCompression();
		for (int i = 0; i < result.length; i++) {
			hash += String.format("%08x", result[i]);
		}
		return hash;
	}

	/**
	 * ����ѹ��
	 */
	private int[] IterativeCompression() {

		// ���ڴ洢�ֶ���Ϣ
		int[] Message = new int[16];
		// ��Ϣ�ܶ���
		int n = this.PaddingMessage.length() / 64;
		// ��ʼ����
		int[] Vi = { 0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600, 0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e };
		int[] Vi1 = new int[8];
		// ѭ��ʱ��ÿ��Vi
		for (int i = 0; i < n; i++) {
			// System.out.println("����ѭ��ÿ��512λ");
			try {
				// ����һ��64�ֽڵ��ַ���
				byte[] bts = this.PaddingMessage.substring(64 * i, 64 * (i + 1)).getBytes(charset);
				// �ֳ�16��int
				for (int j = 0; j < 16; j++) {
					Message[j] = bytesToInt(bts, j * 4);
					// System.out.println("����"+j+":"+Message[j]);
				}
				// ��Ϣ��չ
				int[][] Ws = this.messageExpand(Message);
				/*
				 * System.out.println("W0-W67��"); dump(Ws[0]);
				 * System.out.println("W1-0-W1-63��"); dump(Ws[1]);
				 */
				Vi = this.CF(Vi, Message, Ws[0], Ws[1]);
				Vi1 = Vi;

			} catch (UnsupportedEncodingException e) {
				System.out.println("charset Error In IterativeCompression:" + e.getMessage());
			}
		}
		/*
		 * System.out.println("���ս����"); dump(Vi1);
		 */
		return Vi1;
	}

	/**
	 * ��byteת��int
	 */
	private int bytesToInt(byte[] bts, int index) {
		int result;
		result = (int) ((bts[index] & 0xFF) << 24) | ((bts[index + 1] & 0xFF) << 16) | ((bts[index + 2] & 0xFF) << 8)
				| ((bts[index + 3] & 0xFF));
		return result;
	}

	/**
	 * ��Ϣ��չ Message ��һ��512λ=64�ֽ�=16���ֵ���Ϣ
	 */
	private int[][] messageExpand(int[] Message) {
		int W[] = new int[68];
		int W1[] = new int[64];
		// 1.����Ϣ����Ϊ16����
		for (int i = 0; i < Message.length; i++) {
			W[i] = Message[i];
		}
		/*
		 * System.out.println("���鸳ֵ��W��ǰ16���֣�"); dump(W);
		 */
		// 2.��W��ֵ
		for (int j = 16; j <= 67; j++) {
			W[j] = this.P1(W[j - 16] ^ W[j - 9] ^ CircleLeftShift(W[j - 3], 15)) ^ CircleLeftShift(W[j - 13], 7)
					^ W[j - 6];
		}
		// 3.��W1��ֵ
		for (int j = 0; j <= 63; j++) {
			W1[j] = W[j] ^ W[j + 4];
		}
		// ��W��W1һ�𷵻�
		int result[][] = new int[2][];
		result[0] = W;
		result[1] = W1;
		/*
		 * System.out.println("��Ϣ��չ��������W��"); dump(W);
		 */
		return result;
	}

	/**
	 * ��Ϣѹ������CF
	 * 
	 * @param V
	 *            256���� ʸ��
	 * 
	 * @param Message
	 *            �������Ϣ���飬512λ�����ϵ�B(i)
	 */
	private int[] CF(int V[], int Message[], int W[], int W1[]) {
		/*
		 * System.out.println("��ʼ��Ϣѹ����"); dump(V);
		 */
		int Vn[] = new int[8]; // ������󷵻صĽ��
		int A, B, C, D, E, F, G, H, SS1, SS2, TT1, TT2;
		A = V[0];
		B = V[1];
		C = V[2];
		D = V[3];
		E = V[4];
		F = V[5];
		G = V[6];
		H = V[7];
		for (int j = 0; j <= 63; j++) {
			SS1 = CircleLeftShift((CircleLeftShift(A, 12) + E + CircleLeftShift(T(j), j)), 7);
			SS2 = SS1 ^ CircleLeftShift(A, 12);
			TT1 = FF(A, B, C, j) + D + SS2 + W1[j];
			TT2 = GG(E, F, G, j) + H + SS1 + W[j];
			D = C;
			C = CircleLeftShift(B, 9);
			B = A;
			A = TT1;
			H = G;
			G = CircleLeftShift(F, 19);
			F = E;
			E = P0(TT2);
			//System.out.printf("%d: %08x %08x %08x %08x %08x %08x %08x %08x\n", j, A, B,C,D,E,F,G,H);
		}
		// Vn = ABCDEFGH ^ V
		Vn[0] = A ^ V[0];
		Vn[1] = B ^ V[1];
		Vn[2] = C ^ V[2];
		Vn[3] = D ^ V[3];
		Vn[4] = E ^ V[4];
		Vn[5] = F ^ V[5];
		Vn[6] = G ^ V[6];
		Vn[7] = H ^ V[7];

		return Vn;
	}

	private void dump() {
		System.out.println("========��ʼ��ӡ========");
		try {
			byte bts[] = this.PaddingMessage.getBytes(this.charset);
			for (int i = 0; i < bts.length; i++) {
				if (i % 16 != 0 && i % 2 == 0 && i != 0) {
					System.out.print("  ");
				}
				if (i % 16 == 0 && i != 0) {
					System.out.println();
				}
				System.out.printf("%02x", bts[i]);
			}
		} catch (Exception e) {
			System.out.println("Error Catch");
		}
		System.out.println("\n========������ӡ========");
	}

	/**
	 * ���ַ������Ϊ16������ʽ
	 */
	private static void dump(String str) {
		System.out.println("========��ʼ��ӡ========");
		byte bts[] = str.getBytes();
		for (int i = 0; i < bts.length; i++) {
			if (i % 8 != 0 && i % 2 == 0 && i != 0) {
				System.out.print("  ");
			}
			if (i % 8 == 0 && i != 0) {
				System.out.println();
			}
			System.out.printf("%02x", bts[i]);
		}
		System.out.println("\n========������ӡ========");
	}

	/**
	 * �������������Ϊ16������ʽ
	 */
	private static void dump(int nums[]) {
		System.out.println("========��ʼ��ӡ========");
		for (int i = 0; i < nums.length; i++) {
			if (i % 8 != 0 && i != 0) {
				System.out.print("  ");
			}
			if (i % 8 == 0 && i != 0) {
				System.out.println();
			}
			System.out.printf("%08x", nums[i]);
		}
		System.out.println("\n========������ӡ========");
	}

	/**
	 * ������ת�ַ���
	 */
	private static String longToString(long n) {
		String res = "";
		for (int i = 0; i < 8; i++) {
			int offset = (7 - i) * 8;
			res += (char) ((n >>> offset) & 0xFF);
		}
		return res;
	}

}
