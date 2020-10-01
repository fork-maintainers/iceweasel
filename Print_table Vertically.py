print('PROGRAM FOR PRINTING TABLE VERTICALLY \n')
n=int(input('Enter upto which you want the table : '))
for i in range(1,11):
	for j in range(2,n+1):
		print(j,'*',i,'=', j*i, end='\t')
	print()
