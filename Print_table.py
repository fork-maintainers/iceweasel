print('PROGRAM FOR TABLE')
n=int(input('Enter upto which you want the table :'))
for i in range(1,n+1):
	for j in range(1,11):
		print(i, 'x', j, '=', i*j)
	print()
