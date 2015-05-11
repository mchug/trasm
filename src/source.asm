Data1 segment
	dbVar1 db 10010011b
	dwVar2 dw 0ABCh
	ddVar3 dd 10101010
	Data1 ends
	Data2 segment
	STR4 db 'Hello!'
	ddVar5 dd 10101010
Data2 ends

assume ds:Data1, cs:Code, es:Data2
Code segment
	begin:
		tmp db 55h
		Cli
		Inc STR4[bx]
		Dec al
		Dec ebx
		Add cs:dbVar1[si], 00010001b
		Cmp bx, dwVar2[eax]
		Xor tmp[ebp], cl
		Mov ah, 128
		Or esi, eax
		jb labelJB
	labelUP:
		jmp labelDW
	labelJB:
		jmp labelUP
		jb begin
	labelDW:
Code ends
end begin
